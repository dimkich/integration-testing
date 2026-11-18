package io.github.dimkich.integration.testing.redis.wait;

import io.github.dimkich.integration.testing.redis.RedisKeyspaceEventsAwait;
import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import io.github.dimkich.integration.testing.redis.replication.RedisSyncBarrier;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dedicated RESP2 subscriber for Redis keyspace notifications of one logical connection.
 * <p>
 * Subscribes to one {@code __keyevent@*__:<type>} pattern per event type requested by the
 * annotations targeting the connection, and to the sync channel of the existing
 * {@link RedisSyncBarrier}. A token of this connection published by the replication wait completion
 * proves that every notification published before it has already been counted: Redis delivers
 * messages to a single subscriber in publication order.
 * <p>
 * Reading and counting happen in one thread, so no additional synchronization is needed between
 * receiving a notification and updating the counters.
 */
@Slf4j
public class RedisKeyspaceSubscriber implements Closeable {
    private static final String KEYEVENT_PREFIX = "__keyevent@*__:";

    private final String name;
    private final RedisProperties.Connection connection;
    private final Set<String> eventTypes = new LinkedHashSet<>();
    private final Map<String, Long> observed = new ConcurrentHashMap<>();
    private final Object monitor = new Object();

    /** Last barrier token received from this connection. */
    private String lastSyncToken;

    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private volatile boolean closed;

    public RedisKeyspaceSubscriber(String name, RedisProperties properties,
                                   List<RedisKeyspaceEventsAwait> awaits) {
        this.name = name;
        this.connection = properties.getConnection(name);
        for (RedisKeyspaceEventsAwait await : awaits) {
            eventTypes.addAll(Arrays.asList(await.events()));
        }
    }

    /** Connects and starts the read loop. */
    @PostConstruct
    public void start() {
        try {
            open();
            List<String> patterns = new ArrayList<>();
            for (String type : eventTypes) {
                patterns.add(KEYEVENT_PREFIX + type);
            }
            patterns.add(RedisSyncBarrier.SYNC_CHANNEL);

            if (connection.getPassword() != null) {
                if (connection.getUser() != null) {
                    send("AUTH", connection.getUser(), connection.getPassword());
                } else {
                    send("AUTH", connection.getPassword());
                }
                readReply();
            }
            int database = connection.getDatabase() == null ? 0 : connection.getDatabase();
            send("SELECT", String.valueOf(database));
            readReply();

            String[] subscription = new String[patterns.size() + 1];
            subscription[0] = "PSUBSCRIBE";
            for (int i = 0; i < patterns.size(); i++) {
                subscription[i + 1] = patterns.get(i);
            }
            send(subscription);
            for (int i = 0; i < patterns.size(); i++) {
                readReply();
            }
            socket.setSoTimeout(0);

            Thread reader = new Thread(this::readLoop, "redis-keyspace-" + name);
            reader.setDaemon(true);
            reader.start();
            log.debug("Redis [{}]: keyspace subscriber started for {}", name, eventTypes);
        } catch (IOException e) {
            close();
            throw new IllegalStateException("Failed to start Redis keyspace subscriber for [" + name + "]", e);
        }
    }

    /** Sum of cumulative counts for the given event types. */
    public long total(Set<String> types) {
        synchronized (monitor) {
            long result = 0;
            for (String type : types) {
                result += observed.getOrDefault(type, 0L);
            }
            return result;
        }
    }

    /** Last barrier token received from this connection, or {@code null} if none yet. */
    public String lastSyncToken() {
        synchronized (monitor) {
            return lastSyncToken;
        }
    }

    /**
     * Blocks until a barrier token of this connection different from {@code previous} is received.
     * <p>
     * Redis delivers pub/sub messages to one subscriber in publication order, so receiving a new
     * token proves that every notification published before it has already been counted. Tokens
     * of other connections are ignored even when they share the same Redis instance and channel.
     *
     * @param previous last token already consumed by the caller
     * @param timeoutMs maximum wait in milliseconds
     * @return the new token
     */
    public String awaitSyncTokenNot(String previous, long timeoutMs) {
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
        synchronized (monitor) {
            while (Objects.equals(lastSyncToken, previous)) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    throw new IllegalStateException("Timed out waiting for a Redis sync barrier token on ["
                            + name + "]; the replication barrier may not be running");
                }
                try {
                    monitor.wait(Math.max(1L, remaining / 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for a Redis sync barrier token", e);
                }
            }
            return lastSyncToken;
        }
    }

    @Override
    public void close() {
        closed = true;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // best effort
            }
            socket = null;
        }
    }

    private void open() throws IOException {
        String host = connection.getHost();
        int port = connection.getPort();
        socket = Boolean.TRUE.equals(connection.getSsl())
                ? SSLSocketFactory.getDefault().createSocket(host, port)
                : new Socket(host, port);
        socket.setTcpNoDelay(true);
        int timeout = connection.getTimeoutMs() == null ? 5000 : connection.getTimeoutMs();
        socket.setSoTimeout(timeout);
        in = new BufferedInputStream(socket.getInputStream());
        out = new BufferedOutputStream(socket.getOutputStream());
    }

    private void readLoop() {
        try {
            while (!closed) {
                Object reply = readReply();
                if (reply instanceof List<?> parts && parts.size() >= 4
                        && "pmessage".equals(asString(parts.get(0)))) {
                    onMessage(asString(parts.get(2)), asString(parts.get(3)));
                }
            }
        } catch (IOException e) {
            if (!closed) {
                log.warn("Redis [{}]: keyspace subscriber stopped: {}", name, e.toString());
            }
        }
    }

    private void onMessage(String channel, String payload) {
        synchronized (monitor) {
            if (RedisSyncBarrier.SYNC_CHANNEL.equals(channel)) {
                if (isOwnToken(payload)) {
                    lastSyncToken = payload;
                }
            } else {
                int colon = channel.lastIndexOf(':');
                String type = colon >= 0 ? channel.substring(colon + 1) : channel;
                observed.merge(type, 1L, Long::sum);
            }
            monitor.notifyAll();
        }
    }

    private boolean isOwnToken(String token) {
        int colon = token.lastIndexOf(':');
        return colon > 0 && name.equals(token.substring(0, colon));
    }

    private void send(String... args) throws IOException {
        out.write(('*' + String.valueOf(args.length)).getBytes(StandardCharsets.US_ASCII));
        out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        for (String arg : args) {
            byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
            out.write(('$' + String.valueOf(bytes.length)).getBytes(StandardCharsets.US_ASCII));
            out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
            out.write(bytes);
            out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        }
        out.flush();
    }

    private Object readReply() throws IOException {
        String line = readLine();
        if (line == null) {
            throw new IOException("Connection closed");
        }
        if (line.startsWith("*")) {
            int size = Integer.parseInt(line.substring(1));
            List<Object> result = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                result.add(readReply());
            }
            return result;
        }
        if (line.startsWith("$")) {
            int length = Integer.parseInt(line.substring(1));
            byte[] data = readFully(length);
            readLine(); // trailing CRLF
            return data;
        }
        if (line.startsWith("-")) {
            throw new IOException("Redis error: " + line);
        }
        return line; // +OK, :integer
    }

    private byte[] readFully(int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(data, offset, length - offset);
            if (read < 0) {
                throw new IOException("Connection closed while reading bulk string");
            }
            offset += read;
        }
        return data;
    }

    private String readLine() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (true) {
            int value = in.read();
            if (value < 0) {
                return buffer.size() == 0 ? null : buffer.toString(StandardCharsets.UTF_8);
            }
            if (value == '\r') {
                int next = in.read();
                if (next == '\n') {
                    return buffer.toString(StandardCharsets.UTF_8);
                }
                buffer.write(value);
                if (next >= 0) {
                    buffer.write(next);
                }
                continue;
            }
            buffer.write(value);
        }
    }

    private static String asString(Object value) {
        return value instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : String.valueOf(value);
    }
}

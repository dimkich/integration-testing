package io.github.dimkich.integration.testing.redis.client;

import com.moilioncircle.redis.replicator.Configuration;
import com.moilioncircle.redis.replicator.client.RESP2;
import com.moilioncircle.redis.replicator.client.RESP2Client;
import com.moilioncircle.redis.replicator.util.ByteArray;
import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link RedisClient} backed by {@code redis-replicator}'s {@link RESP2Client}.
 * <p>
 * The underlying client already implements the RESP2 protocol, socket setup (TCP_NODELAY,
 * keepalive, read/connect timeouts, SSL), authentication and pipelining. This class adds the
 * framework contract: lazy connection, a per-client lock, Redis error detection, reply decoding
 * and reconnect on the next command after a transport failure.
 * <p>
 * The client speaks RESP2 only, which keeps compatibility with Redis 5 and newer; the server
 * replies in RESP2 because the client never sends {@code HELLO 3}.
 */
@Slf4j
public class Resp2RedisClient implements RedisClient {
    private static final Charset ASCII = StandardCharsets.US_ASCII;
    private static final byte[] SELECT = "SELECT".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FLUSHALL = "FLUSHALL".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CONFIG = "CONFIG".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PUBLISH = "PUBLISH".getBytes(StandardCharsets.UTF_8);

    private static final int PIPELINE_BATCH_SIZE = 1000;

    private final String name;
    private final RedisProperties.Connection connection;
    private final ReentrantLock lock = new ReentrantLock();

    private RESP2Client client;
    private int database;

    public Resp2RedisClient(String name, RedisProperties properties) {
        this.name = name;
        this.connection = properties.getConnection(name);
        this.database = connection.getDatabase();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getDefaultDatabase() {
        return connection.getDatabase();
    }

    @Override
    public Object execute(byte[] command, byte[]... args) {
        lock.lock();
        try {
            checkNoPipeline();
            return executeInternal(command, args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void select(int database) {
        lock.lock();
        try {
            checkNoPipeline();
            executeInternal(SELECT, ascii(database));
            this.database = database;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void flushAll() {
        lock.lock();
        try {
            checkNoPipeline();
            executeInternal(FLUSHALL);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void configSet(String parameter, String value) {
        lock.lock();
        try {
            checkNoPipeline();
            executeInternal(CONFIG,
                    "SET".getBytes(StandardCharsets.UTF_8),
                    parameter.getBytes(StandardCharsets.UTF_8),
                    value.getBytes(StandardCharsets.UTF_8));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void publish(byte[] channel, byte[] message) {
        lock.lock();
        try {
            checkNoPipeline();
            executeInternal(PUBLISH, channel, message);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("resource")
    public RedisPipeline pipeline() {
        lock.lock();
        try {
            if (lock.getHoldCount() > 1) {
                throw new IllegalStateException(
                        "Nested Redis pipeline for connection [" + name + "] is not supported");
            }
            return new Resp2RedisPipeline(this, connection().newCommand(), PIPELINE_BATCH_SIZE);
        } catch (RuntimeException e) {
            lock.unlock();
            throw e;
        }
    }

    /**
     * Rejects a command issued by a thread that already holds an open pipeline: the pipeline has
     * written commands whose replies are still unread, so a single command would consume the wrong
     * reply and desynchronize the protocol stream.
     * <p>
     * Only the current thread is affected ({@link ReentrantLock#getHoldCount()}); a command from
     * another thread simply waits until the pipeline is closed.
     */
    private void checkNoPipeline() {
        if (lock.getHoldCount() > 1) {
            throw new IllegalStateException(
                    "Redis connection [" + name + "] is busy with an open pipeline");
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            checkNoPipeline();
            closeQuietly();
        } finally {
            lock.unlock();
        }
    }

    void unlockPipeline() {
        lock.unlock();
    }

    void setDatabase(int database) {
        this.database = database;
    }

    /**
     * Drops the broken connection; the next command reconnects and restores the selected database.
     */
    void reset(IOException cause) {
        log.warn("Redis connection [{}] will be re-established after an I/O error: {}",
                name, cause.getMessage());
        closeQuietly();
    }

    @SuppressWarnings("resource")
    private Object executeInternal(byte[] command, byte[]... args) {
        RESP2Client connection = connection();
        try {
            RESP2.Node node = connection.newCommand().invoke(command(command, args));
            return toObject(node, command);
        } catch (IOException e) {
            reset(e);
            throw new RedisClientException(String.format(
                    "Redis command [%s] failed on connection [%s]: %s",
                    text(command), name, e.getMessage()), e);
        }
    }

    private RESP2Client connection() {
        if (client != null) {
            return client;
        }
        String host = connection.getHost();
        Integer port = connection.getPort();
        if (host == null || host.isEmpty()) {
            throw new RedisClientException("Redis host is missing for connection [" + name + "]");
        }
        if (port == null || port <= 0) {
            throw new RedisClientException("Redis port is invalid for connection [" + name + "]: " + port);
        }

        int timeout = connection.getTimeoutMs();
        boolean ssl = connection.getSsl();
        Configuration configuration = Configuration.defaultSetting()
                .setAuthUser(connection.getUser())
                .setAuthPassword(connection.getPassword())
                .setConnectionTimeout(timeout)
                .setReadTimeout(timeout)
                .setSsl(ssl);
        if (ssl) {
            configuration.setSslSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
        }

        RESP2Client created;
        try {
            created = new RESP2Client(host, port, configuration);
        } catch (IOException e) {
            throw new RedisClientException(String.format(
                    "Cannot connect to Redis [%s] at %s:%d: %s", name, host, port, e.getMessage()), e);
        } catch (AssertionError e) {
            throw new RedisClientException(String.format(
                    "Redis authentication failed for connection [%s]: %s", name, e.getMessage()), e);
        }
        client = created;

        if (database != 0) {
            try {
                RESP2.Node node = created.newCommand().invoke(command(SELECT, ascii(database)));
                if (node != null && node.type == RESP2.Type.ERROR) {
                    throw error(SELECT, node);
                }
            } catch (IOException | RuntimeException e) {
                closeQuietly();
                throw new RedisClientException(String.format(
                        "Cannot select Redis database %d for connection [%s]", database, name), e);
            }
        }
        return created;
    }

    private void closeQuietly() {
        RESP2Client current = client;
        client = null;
        if (current != null) {
            try {
                current.close();
            } catch (IOException e) {
                log.debug("Failed to close Redis connection [{}]: {}", name, e.getMessage());
            }
        }
    }

    private RedisClientException error(byte[] command, RESP2.Node node) {
        return new RedisClientException(String.format(
                "Redis error on command [%s] for connection [%s]: %s",
                text(command), name, node.getError()));
    }

    private Object toObject(RESP2.Node node, byte[] command) {
        if (node == null) {
            return null;
        }
        return switch (node.type) {
            case NULL -> null;
            case NUMBER -> node.getNumber();
            case STRING -> toBytes(node.getBytes());
            case ARRAY -> toArray(node.getArray(), command);
            case ERROR -> throw error(command, node);
        };
    }

    private Object[] toArray(RESP2.Node[] nodes, byte[] command) {
        if (nodes == null) {
            return null;
        }
        Object[] result = new Object[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            result[i] = toObject(nodes[i], command);
        }
        return result;
    }

    private static byte[] toBytes(ByteArray value) {
        if (value == null) {
            return null;
        }
        byte[] bytes = value.first();
        if (bytes != null && bytes.length == value.length()) {
            return bytes;
        }
        if (value.length() > Integer.MAX_VALUE) {
            throw new RedisClientException("Redis reply is too large: " + value.length() + " bytes");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) value.length());
        try {
            value.writeTo(out, 0, value.length());
        } catch (IOException e) {
            throw new RedisClientException("Cannot read Redis reply", e);
        }
        return out.toByteArray();
    }

    static byte[][] command(byte[] command, byte[]... args) {
        byte[][] result = new byte[args.length + 1][];
        result[0] = command;
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }

    static byte[] ascii(long value) {
        return Long.toString(value).getBytes(ASCII);
    }

    static String text(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }
}

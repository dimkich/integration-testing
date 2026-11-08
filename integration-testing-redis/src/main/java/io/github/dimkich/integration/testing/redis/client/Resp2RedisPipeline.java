package io.github.dimkich.integration.testing.redis.client;

import com.moilioncircle.redis.replicator.client.RESP2;
import com.moilioncircle.redis.replicator.client.RESP2Client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link RedisPipeline} backed by {@link RESP2Client.Command#post}.
 * <p>
 * The pipeline writes every queued command to the socket immediately and reads the reply later,
 * so a batch travels without a round trip per command. It drains the replies when the batch
 * reaches its maximum size, on {@link #flush()} and on {@link #close()}. This object owns the
 * underlying connection until {@link #close()}.
 * <p>
 * The pipeline accumulates per-field hash expirations and sends them as a single {@code EVAL}
 * at flush time, which keeps the command count low for fixtures with many expiring hash fields.
 */
final class Resp2RedisPipeline implements RedisPipeline {
    private static final byte[] SELECT = "SELECT".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SET = "SET".getBytes(StandardCharsets.UTF_8);
    private static final byte[] DEL = "DEL".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PEXPIREAT = "PEXPIREAT".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HSET = "HSET".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HDEL = "HDEL".getBytes(StandardCharsets.UTF_8);
    private static final byte[] RPUSH = "RPUSH".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SADD = "SADD".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ZADD = "ZADD".getBytes(StandardCharsets.UTF_8);
    private static final byte[] XADD = "XADD".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PFADD = "PFADD".getBytes(StandardCharsets.UTF_8);
    private static final byte[] EVAL = "EVAL".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HASH_TTL_SCRIPT = (
            "for i = 1, #ARGV, 3 do "
                    + "redis.call('HPEXPIREAT', ARGV[i], ARGV[i + 1], 'FIELDS', '1', ARGV[i + 2]) "
                    + "end").getBytes(StandardCharsets.UTF_8);

    /** Maximum number of members, fields or values placed into one Redis command. */
    private static final int MAX_CHUNK = 512;

    private final Resp2RedisClient owner;
    private final RESP2Client.Command batch;
    private final int batchSize;
    private final List<byte[]> hashTtl = new ArrayList<>();

    private int pending;
    private int queuedDatabase = Integer.MIN_VALUE;
    /** First Redis error reply of the batch; reported on flush once every reply has been drained. */
    private String firstError;
    /** Set when the transport fails: the batch is unusable and {@link #close()} must not flush. */
    private boolean failed;
    private boolean closed;

    Resp2RedisPipeline(Resp2RedisClient owner, RESP2Client.Command batch, int batchSize) {
        this.owner = owner;
        this.batch = batch;
        this.batchSize = batchSize;
    }

    @Override
    public void select(int database) {
        // Pending field TTLs belong to the database that was active when they were queued,
        // so send them before switching; otherwise the final EVAL runs in the wrong database.
        postHashTtl();
        queuedDatabase = database;
        post(Resp2RedisClient.command(SELECT, Resp2RedisClient.ascii(database)));
    }

    @Override
    public void set(byte[] key, byte[] value) {
        post(Resp2RedisClient.command(SET, key, value));
    }

    @Override
    public void del(byte[]... keys) {
        postDelChunked(keys);
    }

    @Override
    public void pExpireAt(byte[] key, long timestampMs) {
        post(Resp2RedisClient.command(PEXPIREAT, key, Resp2RedisClient.ascii(timestampMs)));
    }

    @Override
    public void hSet(byte[] key, Map<byte[], byte[]> fieldValues) {
        List<byte[]> pairs = new ArrayList<>();
        for (Map.Entry<byte[], byte[]> entry : fieldValues.entrySet()) {
            pairs.add(entry.getKey());
            pairs.add(entry.getValue());
            if (pairs.size() >= MAX_CHUNK * 2) {
                postHSet(key, pairs);
                pairs.clear();
            }
        }
        if (!pairs.isEmpty()) {
            postHSet(key, pairs);
        }
    }

    @Override
    public void hDel(byte[] key, byte[]... fields) {
        postChunked(HDEL, key, fields);
    }

    @Override
    public void hExpireAt(byte[] key, byte[] field, long timestampMs) {
        hashTtl.add(key);
        hashTtl.add(Resp2RedisClient.ascii(timestampMs));
        hashTtl.add(field);
    }

    @Override
    public void rPush(byte[] key, byte[]... values) {
        postChunked(RPUSH, key, values);
    }

    @Override
    public void sAdd(byte[] key, byte[]... members) {
        postChunked(SADD, key, members);
    }

    @Override
    public void zAdd(byte[] key, List<ScoredMember> members) {
        for (int offset = 0; offset < members.size(); offset += MAX_CHUNK) {
            int length = Math.min(MAX_CHUNK, members.size() - offset);
            byte[][] args = new byte[length * 2 + 1][];
            args[0] = key;
            for (int i = 0; i < length; i++) {
                ScoredMember member = members.get(offset + i);
                args[i * 2 + 1] = Double.toString(member.getScore()).getBytes(StandardCharsets.US_ASCII);
                args[i * 2 + 2] = member.getMember();
            }
            post(Resp2RedisClient.command(ZADD, args));
        }
    }

    @Override
    public void xAdd(byte[] key, byte[] id, Map<byte[], byte[]> fields) {
        // XADD cannot be split: every field must belong to the same entry, so MAX_CHUNK does not apply.
        byte[][] args = new byte[fields.size() * 2 + 2][];
        args[0] = key;
        args[1] = id;
        int index = 2;
        for (Map.Entry<byte[], byte[]> entry : fields.entrySet()) {
            args[index++] = entry.getKey();
            args[index++] = entry.getValue();
        }
        post(Resp2RedisClient.command(XADD, args));
    }

    @Override
    public void pfAdd(byte[] key, byte[]... elements) {
        postChunked(PFADD, key, elements);
    }

    @Override
    public void eval(byte[] script, int numKeys, byte[]... args) {
        byte[][] command = new byte[args.length + 3][];
        command[0] = EVAL;
        command[1] = script;
        command[2] = Integer.toString(numKeys).getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(args, 0, command, 3, args.length);
        post(command);
    }

    @Override
    public void execute(byte[] command, byte[]... args) {
        post(Resp2RedisClient.command(command, args));
    }

    @Override
    public void flush() {
        if (closed) {
            return;
        }
        if (!hashTtl.isEmpty()) {
            postHashTtl();
        }
        drain();
        if (queuedDatabase != Integer.MIN_VALUE) {
            owner.setDatabase(queuedDatabase);
            queuedDatabase = Integer.MIN_VALUE;
        }
        throwIfFailed();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        try {
            if (!failed) {
                flush();
            }
        } finally {
            closed = true;
            owner.unlockPipeline();
        }
    }

    /**
     * Posts {@code command key values...}, splitting the values into batches of {@link #MAX_CHUNK}
     * so that a single Redis command never grows beyond the server limit.
     */
    private void postChunked(byte[] command, byte[] key, byte[]... values) {
        for (int offset = 0; offset < values.length; offset += MAX_CHUNK) {
            int length = Math.min(MAX_CHUNK, values.length - offset);
            byte[][] args = new byte[length + 1][];
            args[0] = key;
            System.arraycopy(values, offset, args, 1, length);
            post(Resp2RedisClient.command(command, args));
        }
    }

    /**
     * Posts {@code DEL key...} in batches of {@link #MAX_CHUNK}, so a large key list never grows
     * beyond the server limit.
     */
    private void postDelChunked(byte[]... keys) {
        for (int offset = 0; offset < keys.length; offset += MAX_CHUNK) {
            int length = Math.min(MAX_CHUNK, keys.length - offset);
            byte[][] args = new byte[length][];
            System.arraycopy(keys, offset, args, 0, length);
            post(Resp2RedisClient.command(DEL, args));
        }
    }

    private void post(byte[]... command) {
        write(command);
        if (pending >= batchSize) {
            flush();
        }
    }

    /**
     * Writes a command to the socket without flushing, so internal helpers can decide when to
     * drain the replies themselves.
     */
    private void write(byte[]... command) {
        ensureOpen();
        try {
            batch.post(node -> {
                if (node != null && node.type == RESP2.Type.ERROR && firstError == null) {
                    firstError = String.format("Redis error on command [%s] for connection [%s]: %s",
                            Resp2RedisClient.text(command[0]), owner.getName(), node.getError());
                }
            }, command);
        } catch (IOException e) {
            failed = true;
            owner.reset(e);
            throw new RedisClientException(String.format(
                    "Redis command [%s] failed on connection [%s]: %s",
                    Resp2RedisClient.text(command[0]), owner.getName(), e.getMessage()), e);
        }
        pending++;
    }

    /** @throws IllegalStateException when the pipeline was already closed */
    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Redis pipeline is already closed");
        }
    }

    private void drain() {
        if (pending == 0) {
            return;
        }
        try {
            batch.get();
        } catch (IOException e) {
            failed = true;
            owner.reset(e);
            throw new RedisClientException(String.format(
                    "Cannot read Redis replies on connection [%s]: %s",
                    owner.getName(), e.getMessage()), e);
        } finally {
            pending = 0;
        }
    }

    private void throwIfFailed() {
        if (firstError == null) {
            return;
        }
        String error = firstError;
        firstError = null;
        throw new RedisClientException(error);
    }

    private void postHashTtl() {
        int step = MAX_CHUNK * 3;
        for (int offset = 0; offset < hashTtl.size(); offset += step) {
            int length = Math.min(step, hashTtl.size() - offset);
            byte[][] command = new byte[length + 3][];
            command[0] = EVAL;
            command[1] = HASH_TTL_SCRIPT;
            command[2] = "0".getBytes(StandardCharsets.US_ASCII);
            for (int i = 0; i < length; i++) {
                command[i + 3] = hashTtl.get(offset + i);
            }
            write(command);
        }
        hashTtl.clear();
    }

    private void postHSet(byte[] key, List<byte[]> pairs) {
        byte[][] args = new byte[pairs.size() + 1][];
        args[0] = key;
        for (int i = 0; i < pairs.size(); i++) {
            args[i + 1] = pairs.get(i);
        }
        post(Resp2RedisClient.command(HSET, args));
    }
}

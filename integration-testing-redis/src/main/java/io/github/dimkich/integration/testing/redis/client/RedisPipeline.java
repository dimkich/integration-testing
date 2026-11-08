package io.github.dimkich.integration.testing.redis.client;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * Write-only pipeline for bulk data loading.
 * <p>
 * Commands are sent to the server without waiting for replies, so a whole test fixture is
 * transferred with a single network round trip per batch. Replies are drained when the batch
 * size is reached, on {@link #flush()} and on {@link #close()}.
 * <p>
 * A pipeline owns the underlying connection until it is closed: use try-with-resources and do
 * not call {@link RedisClient} methods from another thread while a pipeline is open.
 */
public interface RedisPipeline extends Closeable {

    /**
     * Queues {@code SELECT}.
     *
     * @param database zero-based database index
     */
    void select(int database);

    /**
     * Queues {@code SET}.
     *
     * @param key   serialized key
     * @param value serialized value
     */
    void set(byte[] key, byte[] value);

    /**
     * Queues {@code DEL}. Large key lists are split into several commands.
     *
     * @param keys serialized keys
     */
    void del(byte[]... keys);

    /**
     * Queues {@code PEXPIREAT}.
     *
     * @param key         serialized key
     * @param timestampMs expiration instant in milliseconds since epoch
     */
    void pExpireAt(byte[] key, long timestampMs);

    /**
     * Queues {@code HSET}. Large hashes are split into several commands.
     *
     * @param key         serialized key
     * @param fieldValues serialized field-value pairs; the map is only iterated
     */
    void hSet(byte[] key, Map<byte[], byte[]> fieldValues);

    /**
     * Queues {@code HDEL}. Large field lists are split into several commands.
     *
     * @param key    serialized key
     * @param fields serialized field names
     */
    void hDel(byte[] key, byte[]... fields);

    /**
     * Queues {@code HPEXPIREAT} for a single hash field.
     * <p>
     * All queued field expirations are collapsed into one {@code EVAL} at flush time.
     *
     * @param key         serialized hash key
     * @param field       serialized field name
     * @param timestampMs expiration instant in milliseconds since epoch
     */
    void hExpireAt(byte[] key, byte[] field, long timestampMs);

    /**
     * Queues {@code RPUSH}. Large value lists are split into several commands.
     *
     * @param key    serialized key
     * @param values serialized values
     */
    void rPush(byte[] key, byte[]... values);

    /**
     * Queues {@code SADD}. Large member lists are split into several commands.
     *
     * @param key     serialized key
     * @param members serialized members
     */
    void sAdd(byte[] key, byte[]... members);

    /**
     * Queues {@code ZADD}. Large member lists are split into several commands.
     *
     * @param key     serialized key
     * @param members scored members
     */
    void zAdd(byte[] key, List<ScoredMember> members);

    /**
     * Queues {@code XADD}.
     * <p>
     * All fields of one entry are sent in a single command: {@code XADD} cannot be split, so the
     * per-command chunk limit does not apply here.
     *
     * @param key    serialized stream key
     * @param id     stream entry id, for example {@code 1698400000000-0}
     * @param fields serialized field-value pairs; the map is only iterated
     */
    void xAdd(byte[] key, byte[] id, Map<byte[], byte[]> fields);

    /**
     * Queues {@code PFADD}. Large element lists are split into several commands.
     *
     * @param key      serialized key
     * @param elements serialized elements
     */
    void pfAdd(byte[] key, byte[]... elements);

    /**
     * Queues {@code EVAL}.
     *
     * @param script  Lua script
     * @param numKeys number of keys at the beginning of {@code args}
     * @param args    keys followed by arguments
     */
    void eval(byte[] script, int numKeys, byte[]... args);

    /**
     * Queues an arbitrary command.
     *
     * @param command command name
     * @param args    raw command arguments
     */
    void execute(byte[] command, byte[]... args);

    /**
     * Sends queued commands and drains all replies. Throws when any reply is a Redis error; every
     * reply is still drained so the connection stays usable. A transport failure resets the
     * connection instead.
     */
    void flush();

    /** Flushes remaining commands and releases the connection. */
    @Override
    void close();
}

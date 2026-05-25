package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.redis.model.RedisHyperLogLog;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import org.springframework.data.redis.connection.RedisConnection;

/**
 * {@link RedisDataAccessor} that writes {@link RedisHyperLogLog} values to Redis.
 * <p>
 * Replaces the target key atomically: deletes the key, then populates it with
 * {@code PFADD} using the schema value codec from {@link RedisDataSchema}.
 */
public class HyperLogLogDataAccessor implements RedisDataAccessor<RedisHyperLogLog> {

    /** {@inheritDoc} */
    @Override
    public Class<RedisHyperLogLog> getSupportedClass() {
        return RedisHyperLogLog.class;
    }

    /**
     * Writes a HyperLogLog to Redis, replacing any existing value at {@code key}.
     * <p>
     * Empty HyperLogLogs only delete the key. Non-empty values are stored with a single
     * {@code PFADD} after serializing each element through the schema value codec.
     *
     * @param key    serialized Redis key
     * @param data   elements to add to the HyperLogLog
     * @param conn   connection used for commands
     * @param schema codecs for HyperLogLog element values
     */
    @Override
    public void store(byte[] key, RedisHyperLogLog data, RedisConnection conn, RedisDataSchema schema) {
        conn.keyCommands().del(key);
        if (data.isEmpty()) {
            return;
        }

        byte[][] rawElements = data.stream()
                .map(schema.getValueCodec()::serialize)
                .toArray(byte[][]::new);

        conn.hyperLogLogCommands().pfAdd(key, rawElements);
    }
}
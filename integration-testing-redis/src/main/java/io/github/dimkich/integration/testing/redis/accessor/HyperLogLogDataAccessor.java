package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.redis.client.RedisPipeline;
import io.github.dimkich.integration.testing.redis.model.RedisHyperLogLog;
import io.github.dimkich.integration.testing.redis.serde.RedisDataSchema;

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
     * Empty HyperLogLogs only delete the key. Non-empty values are stored with
     * {@code PFADD} after serializing each element through the schema value codec.
     *
     * @param key      serialized Redis key
     * @param data     elements to add to the HyperLogLog
     * @param pipeline pipeline used for commands
     * @param schema   codecs for HyperLogLog element values
     */
    @Override
    public void store(byte[] key, RedisHyperLogLog data, RedisPipeline pipeline, RedisDataSchema schema) {
        pipeline.del(key);
        if (data.isEmpty()) {
            return;
        }

        byte[][] rawElements = data.stream()
                .map(schema.getValueCodec()::serialize)
                .toArray(byte[][]::new);

        pipeline.pfAdd(key, rawElements);
    }
}

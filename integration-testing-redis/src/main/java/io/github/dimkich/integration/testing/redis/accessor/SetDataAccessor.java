package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.redis.client.RedisPipeline;
import io.github.dimkich.integration.testing.redis.model.RedisSet;
import io.github.dimkich.integration.testing.redis.serde.RedisDataSchema;

/**
 * {@link RedisDataAccessor} that writes {@link RedisSet} values to Redis.
 * <p>
 * Replaces the target key atomically: deletes the key, then populates it with
 * {@code SADD} using the value codec from {@link RedisDataSchema}.
 */
public class SetDataAccessor implements RedisDataAccessor<RedisSet> {

    /** {@inheritDoc} */
    @Override
    public Class<RedisSet> getSupportedClass() {
        return RedisSet.class;
    }

    /**
     * Writes a set to Redis, replacing any existing value at {@code key}.
     * <p>
     * Empty sets only delete the key. Non-empty sets are stored with {@code SADD};
     * member order in the model is not preserved in Redis.
     *
     * @param key      serialized Redis key
     * @param data     set members to store
     * @param pipeline pipeline used for commands
     * @param schema   codec for set member values
     */
    @Override
    public void store(byte[] key, RedisSet data, RedisPipeline pipeline, RedisDataSchema schema) {
        pipeline.del(key);
        if (data.isEmpty()) {
            return;
        }
        byte[][] raw = data.stream()
                .map(schema.getValueCodec()::serialize)
                .toArray(byte[][]::new);
        pipeline.sAdd(key, raw);
    }
}

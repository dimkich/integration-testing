package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.redis.client.RedisPipeline;
import io.github.dimkich.integration.testing.redis.serde.RedisDataSchema;

/**
 * Persists plain (non-{@link io.github.dimkich.integration.testing.redis.model.RedisValue RedisValue})
 * objects as Redis string values.
 * <p>
 * Invoked by {@link RedisAccessorCoordinator} when test data is not a typed
 * {@code RedisValue}; typed values are routed to {@link RedisDataAccessor} implementations
 * instead.
 */
public class StringDataAccessor {

    /**
     * Writes {@code data} to Redis at {@code keyRaw} using {@code SET}.
     * <p>
     * If {@code data} is already a {@code byte[]}, it is stored as-is. Otherwise, it is
     * serialized with {@link RedisDataSchema#getValueCodec()}.
     *
     * @param keyRaw   encoded Redis key
     * @param data     value to store (raw bytes or a codec-serializable object)
     * @param pipeline pipeline used for writes
     * @param schema   provides the value codec for non-byte[] data
     */
    public void store(byte[] keyRaw, Object data, RedisPipeline pipeline, RedisDataSchema schema) {
        if (data instanceof byte[] bytes) {
            pipeline.set(keyRaw, bytes);
        } else {
            pipeline.set(keyRaw, schema.getValueCodec().serialize(data));
        }
    }
}

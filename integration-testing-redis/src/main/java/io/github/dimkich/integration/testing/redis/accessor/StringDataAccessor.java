package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import org.springframework.data.redis.connection.RedisConnection;

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
     * @param keyRaw encoded Redis key
     * @param data   value to store (raw bytes or a codec-serializable object)
     * @param conn   connection used for the write
     * @param schema provides the value codec for non-byte[] data
     */
    public void store(byte[] keyRaw, Object data, RedisConnection conn, RedisDataSchema schema) {
        if (data instanceof byte[] bytes) {
            conn.stringCommands().set(keyRaw, bytes);
        } else {
            conn.stringCommands().set(keyRaw, schema.getValueCodec().serialize(data));
        }
    }
}


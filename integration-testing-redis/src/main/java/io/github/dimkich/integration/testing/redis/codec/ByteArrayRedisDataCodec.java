package io.github.dimkich.integration.testing.redis.codec;

/**
 * {@link RedisDataCodec} implementation that preserves raw bytes without transformation.
 * <p>
 * Deserialization returns the input byte array unchanged. Serialization returns
 * {@code byte[]} values as-is; other non-null objects are encoded via
 * {@link Object#toString()} using the platform default charset.
 * <p>
 * Used by {@link io.github.dimkich.integration.testing.redis.schema.ByteArrayRedisDataSchema}
 * when Redis values should remain opaque binary data.
 */
public class ByteArrayRedisDataCodec implements RedisDataCodec {

    /**
     * Returns the raw bytes unchanged.
     *
     * @param data raw bytes from Redis
     * @return the same byte array reference, or null if {@code data} is null
     */
    @Override
    public Object deserialize(byte[] data) {
        return data;
    }

    /**
     * Serializes an object to raw bytes for Redis storage.
     *
     * @param object a {@code byte[]} to store as-is, or any other value encoded via
     *               {@link Object#toString()}, may be null
     * @return the serialized bytes, or null if {@code object} is null
     */
    @Override
    public byte[] serialize(Object object) {
        if (object instanceof byte[]) {
            return (byte[]) object;
        }
        return object == null ? null : object.toString().getBytes();
    }
}

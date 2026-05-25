package io.github.dimkich.integration.testing.redis.codec;

/**
 * Encoder/decoder for Redis values, converting between raw bytes and Java objects.
 * <p>
 * Implementations are used by {@link io.github.dimkich.integration.testing.redis.schema.RedisDataSchema}
 * for value, hash key, and hash value codecs. Common implementations include
 * {@link StringRedisDataCodec}, {@link SpringDataCodec}, and {@link RedissonDataCodec}.
 */
public interface RedisDataCodec {

    /**
     * Deserializes raw Redis bytes into a Java object.
     *
     * @param data raw bytes from Redis, may be null or empty
     * @return the deserialized object, or null if the input cannot be decoded
     */
    Object deserialize(byte[] data);

    /**
     * Serializes a Java object into raw bytes for storage in Redis.
     *
     * @param object the object to serialize, may be null
     * @return the serialized bytes, or null if the input cannot be encoded
     */
    byte[] serialize(Object object);
}

package io.github.dimkich.integration.testing.redis.schema;

import io.github.dimkich.integration.testing.redis.codec.RedisDataCodec;

/**
 * Defines the serialization schema for Redis data.
 * <p>
 * A schema specifies which {@link RedisDataCodec codecs} are used for value, hash key,
 * and hash value serialization. Different Redis clients (e.g. Spring Data Redis, Redisson)
 * may use distinct strategies per data type.
 *
 * @see RedisDataCodec
 * @see StringRedisDataSchema
 * @see ByteArrayRedisDataSchema
 * @see ComposedRedisDataSchema
 * @see RedisDataSchemaAdapter
 * @see io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaMetadata
 */
public interface RedisDataSchema {
    /**
     * Returns the codec for string values and for elements of list, set, zset, and stream.
     *
     * @return the value codec
     */
    RedisDataCodec getValueCodec();

    /**
     * Returns the codec for hash field keys.
     *
     * @return the hash key codec
     */
    RedisDataCodec getHashKeyCodec();

    /**
     * Returns the codec for hash field values.
     *
     * @return the hash value codec
     */
    RedisDataCodec getHashValueCodec();
}

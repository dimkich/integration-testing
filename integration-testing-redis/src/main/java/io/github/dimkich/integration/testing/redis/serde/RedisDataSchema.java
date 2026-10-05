package io.github.dimkich.integration.testing.redis.serde;

import lombok.Data;

/**
 * Serialization schema for Redis data: separate {@link RedisDataCodec codecs} for value,
 * hash key and hash value serialization.
 * <p>
 * Different Redis clients (e.g. Spring Data Redis, Redisson) may use distinct strategies per
 * data type; the schema is assembled by {@link RedisSchemaSerdeFactory}.
 *
 * @see RedisDataCodec
 * @see io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaMetadata
 */
@Data
public class RedisDataSchema {
    /** Codec for string values and for elements of list, set, zset, and stream. */
    private final RedisDataCodec valueCodec;

    /** Codec for hash field keys. */
    private final RedisDataCodec hashKeyCodec;

    /** Codec for hash field values. */
    private final RedisDataCodec hashValueCodec;
}

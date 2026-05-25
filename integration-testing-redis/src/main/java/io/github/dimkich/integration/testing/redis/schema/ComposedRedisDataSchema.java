package io.github.dimkich.integration.testing.redis.schema;

import io.github.dimkich.integration.testing.redis.codec.RedisDataCodec;
import lombok.Data;

/**
 * {@link RedisDataSchema} implementation that composes separate codecs for value, hash key,
 * and hash value serialization.
 * <p>
 * Used when Redis stores data with different serialization strategies per data type,
 * e.g. Spring Data Redis templates with distinct value, hash key, and hash value serializers.
 *
 * @see RedisDataSchema
 * @see RedisDataCodec
 * @see SpringDataSchemaAdapter
 * @see RedissonSchemaAdapter
 * @see io.github.dimkich.integration.testing.redis.registry.BinaryEnvelopeService
 */
@Data
public class ComposedRedisDataSchema implements RedisDataSchema {
    /** Codec for string values and for elements of list, set, zset, and stream. */
    private final RedisDataCodec valueCodec;

    /** Codec for hash field keys. */
    private final RedisDataCodec hashKeyCodec;

    /** Codec for hash field values. */
    private final RedisDataCodec hashValueCodec;
}

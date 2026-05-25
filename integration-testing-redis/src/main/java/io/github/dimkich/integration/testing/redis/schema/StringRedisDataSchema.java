package io.github.dimkich.integration.testing.redis.schema;

import io.github.dimkich.integration.testing.redis.codec.RedisDataCodec;
import io.github.dimkich.integration.testing.redis.codec.StringRedisDataCodec;

/**
 * {@link RedisDataSchema} implementation that uses string (UTF-8) encoding for all
 * Redis data types.
 * <p>
 * This schema delegates to {@link StringRedisDataCodec} for value, hash key, and
 * hash value serialization. It is suitable when Redis keys and values are plain
 * strings or when compatibility with simple string-based clients is required.
 *
 * @see RedisDataSchema
 * @see StringRedisDataCodec
 * @see ByteArrayRedisDataSchema
 */
public class StringRedisDataSchema implements RedisDataSchema {
    private final RedisDataCodec stringDecoder = new StringRedisDataCodec();

    /** {@inheritDoc} */
    @Override
    public RedisDataCodec getValueCodec() {
        return stringDecoder;
    }

    /** {@inheritDoc} */
    @Override
    public RedisDataCodec getHashKeyCodec() {
        return stringDecoder;
    }

    /** {@inheritDoc} */
    @Override
    public RedisDataCodec getHashValueCodec() {
        return stringDecoder;
    }
}
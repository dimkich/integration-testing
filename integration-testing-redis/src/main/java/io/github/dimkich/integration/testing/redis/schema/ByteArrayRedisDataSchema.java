package io.github.dimkich.integration.testing.redis.schema;

import io.github.dimkich.integration.testing.redis.codec.ByteArrayRedisDataCodec;
import io.github.dimkich.integration.testing.redis.codec.RedisDataCodec;

/**
 * {@link RedisDataSchema} implementation that preserves raw bytes for all Redis data types.
 * <p>
 * This schema delegates to {@link ByteArrayRedisDataCodec} for value, hash key, and hash value
 * serialization. Use it when Redis values should remain opaque binary data rather than UTF-8
 * strings (contrast with {@link StringRedisDataSchema}).
 *
 * @see RedisDataSchema
 * @see ByteArrayRedisDataCodec
 */
public class ByteArrayRedisDataSchema implements RedisDataSchema {
    private final RedisDataCodec codec = new ByteArrayRedisDataCodec();

    /** {@inheritDoc} */
    @Override
    public RedisDataCodec getValueCodec() {
        return codec;
    }

    /** {@inheritDoc} */
    @Override
    public RedisDataCodec getHashKeyCodec() {
        return codec;
    }

    /** {@inheritDoc} */
    @Override
    public RedisDataCodec getHashValueCodec() {
        return codec;
    }
}

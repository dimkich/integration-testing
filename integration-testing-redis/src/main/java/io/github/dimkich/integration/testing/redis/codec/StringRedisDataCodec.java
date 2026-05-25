package io.github.dimkich.integration.testing.redis.codec;

import java.nio.charset.StandardCharsets;

/**
 * {@link RedisDataCodec} implementation that uses UTF-8 for string serialization
 * and deserialization.
 * <p>
 * Bytes are decoded to {@link String} via {@link StandardCharsets#UTF_8}, and
 * objects are encoded by calling {@link Object#toString()} and encoding the
 * result in UTF-8.
 */
public class StringRedisDataCodec implements RedisDataCodec {
    @Override
    public Object deserialize(byte[] data) {
        return data == null ? null : new String(data, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] serialize(Object object) {
        return object == null ? null : object.toString().getBytes(StandardCharsets.UTF_8);
    }
}

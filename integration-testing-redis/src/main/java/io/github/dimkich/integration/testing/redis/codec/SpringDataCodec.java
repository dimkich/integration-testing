package io.github.dimkich.integration.testing.redis.codec;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * {@link RedisDataCodec} implementation that delegates serialization and deserialization
 * to Spring Data Redis's {@link RedisSerializer}.
 * <p>
 * Use this codec when your application already uses Spring Data Redis serializers
 * (e.g. {@link org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer}
 * or {@link org.springframework.data.redis.serializer.JdkSerializationRedisSerializer})
 * and you want integration tests to use the same serialization format as production.
 */
@RequiredArgsConstructor
public class SpringDataCodec implements RedisDataCodec {
    private final RedisSerializer<?> redisSerializer;

    /**
     * {@inheritDoc}
     * <p>
     * Delegates to the configured Spring Data Redis serializer.
     */
    @Override
    public Object deserialize(byte[] data) {
        return data == null ? null : redisSerializer.deserialize(data);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates to the configured Spring Data Redis serializer.
     */
    @Override
    @SuppressWarnings("unchecked")
    public byte[] serialize(Object object) {
        return object == null ? null : ((RedisSerializer<Object>) redisSerializer).serialize(object);
    }
}

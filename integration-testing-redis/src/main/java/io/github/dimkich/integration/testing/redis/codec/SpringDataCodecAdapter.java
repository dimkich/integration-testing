package io.github.dimkich.integration.testing.redis.codec;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisOperations;

/**
 * {@link RedisDataCodecAdapter} that adapts Spring Data Redis {@link RedisOperations}
 * instances into {@link RedisDataCodec}.
 * <p>
 * Extracts the key serializer from {@code RedisOperations} and wraps it in a
 * {@link SpringDataCodec}, allowing integration tests to use the same serialization
 * format as the application.
 * <p>
 * Only active when Spring Data Redis is on the classpath (conditioned on
 * {@code org.springframework.data.redis.core.RedisOperations}).
 */
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisOperations")
public class SpringDataCodecAdapter implements RedisDataCodecAdapter {

    /**
     * Attempts to create a {@link RedisDataCodec} from a Spring Data Redis bean.
     * <p>
     * If the bean is a {@link RedisOperations} instance, returns a
     * {@link SpringDataCodec} backed by its key serializer.
     *
     * @param bean the bean to adapt (e.g. {@link RedisOperations})
     * @return a {@link RedisDataCodec} if the bean is {@link RedisOperations},
     *         otherwise {@code null}
     */
    @Override
    public RedisDataCodec tryCreateCodec(Object bean) {
        if (bean instanceof RedisOperations<?, ?> ops) {
            return new SpringDataCodec(ops.getKeySerializer());
        }
        return null;
    }
}

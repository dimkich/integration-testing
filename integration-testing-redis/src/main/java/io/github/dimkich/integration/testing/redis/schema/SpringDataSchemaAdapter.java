package io.github.dimkich.integration.testing.redis.schema;

import io.github.dimkich.integration.testing.redis.codec.SpringDataCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisOperations;

/**
 * {@link RedisDataSchemaAdapter} that creates a {@link RedisDataSchema} from Spring Data Redis
 * {@link RedisOperations} instances.
 * <p>
 * Supports beans that implement {@link RedisOperations}. The adapter builds a
 * {@link ComposedRedisDataSchema} with {@link SpringDataCodec}s wrapping the value, hash key,
 * and hash value serializers from the underlying {@code RedisOperations}.
 * <p>
 * This adapter is only active when Spring Data Redis is on the classpath (see
 * {@code ConditionalOnClass}).
 *
 * @see RedisDataSchemaAdapter
 * @see RedisDataSchema
 * @see ComposedRedisDataSchema
 * @see SpringDataCodec
 */
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisOperations")
public class SpringDataSchemaAdapter implements RedisDataSchemaAdapter {

    /**
     * {@inheritDoc}
     * <p>
     * Returns a schema if the bean is a {@link RedisOperations}; otherwise {@code null}.
     */
    @Override
    public RedisDataSchema tryCreateSchema(Object bean) {
        if (bean instanceof RedisOperations<?, ?> ops) {
            return new ComposedRedisDataSchema(
                    new SpringDataCodec(ops.getValueSerializer()),
                    new SpringDataCodec(ops.getHashKeySerializer()),
                    new SpringDataCodec(ops.getHashValueSerializer())
            );
        }
        return null;
    }
}

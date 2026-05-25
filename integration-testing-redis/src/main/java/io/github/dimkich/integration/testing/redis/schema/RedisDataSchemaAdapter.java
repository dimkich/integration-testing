package io.github.dimkich.integration.testing.redis.schema;

/**
 * Adapts a Redis client-specific bean (e.g. {@code RedisOperations}, {@code Codec}) to a
 * {@link RedisDataSchema}.
 * <p>
 * Multiple adapters may be registered; {@link io.github.dimkich.integration.testing.redis.registry.RedisObjectFactory}
 * tries them in order when resolving {@link io.github.dimkich.integration.testing.redis.config.RedisProperties.Schema}
 * bean or class references. Implementations should return {@code null} when the bean type is not supported.
 *
 * @see RedisDataSchema
 * @see SpringDataSchemaAdapter
 * @see RedissonSchemaAdapter
 * @see io.github.dimkich.integration.testing.redis.registry.RedisObjectFactory
 */
public interface RedisDataSchemaAdapter {

    /**
     * Attempts to create a {@link RedisDataSchema} from the given bean.
     *
     * @param bean the Redis client bean to adapt (e.g. a serializer or codec)
     * @return a schema if this adapter supports the bean type, otherwise {@code null}
     */
    RedisDataSchema tryCreateSchema(Object bean);
}

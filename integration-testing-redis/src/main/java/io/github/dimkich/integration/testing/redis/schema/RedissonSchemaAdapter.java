package io.github.dimkich.integration.testing.redis.schema;

import io.github.dimkich.integration.testing.redis.codec.RedissonDataCodec;
import org.redisson.client.codec.Codec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import java.lang.reflect.Method;

/**
 * {@link RedisDataSchemaAdapter} that creates a {@link RedisDataSchema} from Redisson
 * {@link Codec} instances.
 * <p>
 * Supports beans that are either a {@code Codec} directly or that expose a {@code getCodec()}
 * method returning a Redisson codec. The adapter builds a {@link ComposedRedisDataSchema} with
 * {@link RedissonDataCodec}s wrapping the value, map key, and map value encoder/decoder pairs
 * from the underlying Redisson codec.
 * <p>
 * This adapter is only active when Redisson is on the classpath (see {@code ConditionalOnClass}).
 *
 * @see RedisDataSchemaAdapter
 * @see RedisDataSchema
 * @see ComposedRedisDataSchema
 * @see RedissonDataCodec
 */
@ConditionalOnClass(name = "org.redisson.client.codec.Codec")
public class RedissonSchemaAdapter implements RedisDataSchemaAdapter {

    /**
     * {@inheritDoc}
     * <p>
     * Returns a schema if the bean is a Redisson {@link Codec} or exposes {@code getCodec()}
     * returning one; otherwise {@code null}.
     */
    @Override
    public RedisDataSchema tryCreateSchema(Object bean) {
        if (bean instanceof Codec codec) {
            return createSchemaFromCodec(codec);
        }
        Object result = null;
        try {
            Method getCodecMethod = bean.getClass().getMethod("getCodec");
            result = getCodecMethod.invoke(bean);
        } catch (Exception ignore) {
        }
        if (result instanceof Codec codec) {
            return createSchemaFromCodec(codec);
        }

        return null;
    }

    /**
     * Builds a {@link ComposedRedisDataSchema} from the Redisson codec's value, map key,
     * and map value encoder/decoder pairs.
     *
     * @param codec the Redisson codec
     * @return the composed schema
     */
    private RedisDataSchema createSchemaFromCodec(org.redisson.client.codec.Codec codec) {
        return new ComposedRedisDataSchema(
                new RedissonDataCodec(codec.getValueEncoder(), codec.getValueDecoder()),
                new RedissonDataCodec(codec.getMapKeyEncoder(), codec.getMapKeyDecoder()),
                new RedissonDataCodec(codec.getMapValueEncoder(), codec.getMapValueDecoder())
        );
    }
}


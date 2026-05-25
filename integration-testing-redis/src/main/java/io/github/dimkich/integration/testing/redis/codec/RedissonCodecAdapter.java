package io.github.dimkich.integration.testing.redis.codec;

import org.redisson.client.codec.Codec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;

/**
 * {@link RedisDataCodecAdapter} that adapts Redisson {@link Codec} instances into
 * {@link RedisDataCodec}.
 * <p>
 * Supports beans that either implement {@code Codec} directly or expose one via a
 * {@code getCodec()} method (e.g. Redisson {@code Config}, {@code RedissonClient}).
 * <p>
 * Only active when Redisson is on the classpath (conditioned on
 * {@code org.redisson.client.codec.Codec}).
 */
@ConditionalOnClass(name = "org.redisson.client.codec.Codec")
public class RedissonCodecAdapter implements RedisDataCodecAdapter {

    /**
     * Attempts to create a {@link RedisDataCodec} from a Redisson {@link Codec}.
     * <p>
     * First checks if the bean is a {@code Codec} instance. If not, invokes
     * {@code getCodec()} on the bean via reflection and adapts the result.
     *
     * @param bean the bean to adapt (e.g. a {@code Codec} or object with {@code getCodec()})
     * @return a {@link RedisDataCodec} wrapping the Redisson codec, or {@code null} if
     *         the bean cannot be adapted
     */
    @Override
    public RedisDataCodec tryCreateCodec(Object bean) {
        if (bean instanceof Codec codec) {
            return new RedissonDataCodec(codec.getValueEncoder(), codec.getValueDecoder());
        }
        Object result = null;
        try {
            Method getCodecMethod = ClassUtils.getUserClass(bean).getMethod("getCodec");
            result = getCodecMethod.invoke(bean);
        } catch (Exception ignored) {
        }
        if (result instanceof Codec codec) {
            return new RedissonDataCodec(codec.getValueEncoder(), codec.getValueDecoder());
        }
        return null;
    }
}


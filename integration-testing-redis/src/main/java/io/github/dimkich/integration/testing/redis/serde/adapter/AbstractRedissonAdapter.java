package io.github.dimkich.integration.testing.redis.serde.adapter;

import io.github.dimkich.integration.testing.redis.serde.RedisComponentRole;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import lombok.Getter;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Base of the Redisson source adapters: resolves a {@link Codec} from a {@link Codec} bean, a
 * {@link RedissonClient} or any bean exposing a codec via {@code getCodec()} (e.g. Redisson
 * {@code Config}), and extracts the encoder/decoder pair of the requested schema slot as a
 * {@link RedissonSlotCodec}.
 * <p>
 * Registered for the {@code Object} source, so foreign sources yield {@code null} and the
 * manager tries the next adapter. A failure of the {@code getCodec()} call itself is not
 * swallowed: the real cause is propagated.
 */
@Getter
abstract class AbstractRedissonAdapter<I, O>
        implements TestSerdeAdapter<Object, I, O, TestSerdeContext, RedisComponentRole, StandardSerdeProperties> {

    private final Class<Object> sourceClass = Object.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;

    /**
     * Extracts the encoder/decoder pair of the requested slot from the resolved codec: map key
     * encoders/decoders for a hash field name, map value ones for a hash field value, and plain
     * value ones for any other slot or a request without a role.
     *
     * @param source Redisson source
     * @param role   requested component slot, may be {@code null}
     * @return the slot codec, or {@code null} if the source is not Redisson-like
     */
    @Nullable
    protected final RedissonSlotCodec resolveCodec(Object source, @Nullable RedisComponentRole role) {
        Codec codec = resolve(source);
        if (codec == null) {
            return null;
        }
        return switch (role == null ? RedisComponentRole.VALUE : role) {
            case VALUE -> new RedissonSlotCodec(codec.getValueEncoder(), codec.getValueDecoder());
            case HASH_KEY -> new RedissonSlotCodec(codec.getMapKeyEncoder(), codec.getMapKeyDecoder());
            case HASH_VALUE -> new RedissonSlotCodec(codec.getMapValueEncoder(), codec.getMapValueDecoder());
        };
    }

    @Nullable
    private Codec resolve(Object source) {
        if (source instanceof Codec codec) {
            return codec;
        }
        if (source instanceof RedissonClient client) {
            return client.getConfig().getCodec();
        }
        Method getCodecMethod = findGetCodec(source);
        if (getCodecMethod == null) {
            return null;
        }
        Object result;
        try {
            result = getCodecMethod.invoke(source);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(String.format(
                    "Failed to get a Redisson Codec from [%s]: getCodec() threw [%s]",
                    source.getClass().getName(), cause == null ? e : cause.getClass().getSimpleName()),
                    cause == null ? e : cause);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format(
                    "Cannot call getCodec() on [%s] while resolving a Redisson Codec source",
                    source.getClass().getName()), e);
        }
        return result instanceof Codec codec ? codec : null;
    }

    @Nullable
    private Method findGetCodec(Object source) {
        try {
            return ClassUtils.getUserClass(source).getMethod("getCodec");
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}

package io.github.dimkich.integration.testing.redis.serde.adapter;

import io.github.dimkich.integration.testing.redis.serde.RedisComponentRole;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Base of the Spring Data Redis serializer adapters.
 * <p>
 * The adapter works with any bean implementing
 * {@code org.springframework.data.redis.serializer.RedisSerializer} but does not reference the
 * class at compile time: the source class is loaded by name and {@code serialize}/{@code
 * deserialize} are invoked reflectively. This keeps Spring Data out of the module dependencies
 * while still allowing applications to reuse their serializer beans in Redis schemas.
 * <p>
 * Registered for the {@code Object} source, so foreign sources yield {@code null} and the
 * manager tries the next adapter.
 */
@Getter
abstract class AbstractSpringDataAdapter<I, O>
        implements TestSerdeAdapter<Object, I, O, TestSerdeContext, RedisComponentRole, StandardSerdeProperties> {

    private static final String SERIALIZER_CLASS = "org.springframework.data.redis.serializer.RedisSerializer";

    private static final Class<Object> SOURCE_CLASS = loadSourceClass();

    private final Class<Object> sourceClass = SOURCE_CLASS;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;

    /**
     * Calls {@code RedisSerializer.serialize(Object)} on the source bean.
     *
     * @param source serializer bean
     * @param value  value to serialize
     * @return serialized bytes, or {@code null} when the serializer returns {@code null}
     */
    @Nullable
    protected static byte[] serialize(Object source, Object value) {
        return (byte[]) invoke(source, "serialize", Object.class, value);
    }

    /**
     * Calls {@code RedisSerializer.deserialize(byte[])} on the source bean.
     *
     * @param source serializer bean
     * @param value  serialized bytes
     * @return deserialized value
     */
    @Nullable
    protected static Object deserialize(Object source, byte[] value) {
        return invoke(source, "deserialize", byte[].class, value);
    }

    private static Object invoke(Object source, String name, Class<?> parameterType, Object argument) {
        Method method;
        try {
            method = source.getClass().getMethod(name, parameterType);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(String.format(
                    "Bean [%s] does not expose Spring Data RedisSerializer method [%s(%s)]",
                    source.getClass().getName(), name, parameterType.getSimpleName()), e);
        }
        try {
            return method.invoke(source, argument);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(String.format(
                    "Spring Data RedisSerializer method [%s] failed on bean [%s]",
                    name, source.getClass().getName()), cause == null ? e : cause);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format(
                    "Cannot call Spring Data RedisSerializer method [%s] on bean [%s]",
                    name, source.getClass().getName()), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<Object> loadSourceClass() {
        try {
            return (Class<Object>) Class.forName(SERIALIZER_CLASS);
        } catch (ClassNotFoundException e) {
            // Spring Data is absent: no bean can match this type, so the adapter never claims a source.
            return (Class<Object>) (Class<?>) Void.class;
        }
    }
}

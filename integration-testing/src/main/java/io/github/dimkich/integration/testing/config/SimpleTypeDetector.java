package io.github.dimkich.integration.testing.config;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.github.dimkich.integration.testing.date.time.PeriodDuration;
import io.github.dimkich.integration.testing.format.util.JacksonUtils;
import org.springframework.beans.BeanUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects whether a value type is scalar or structured.
 * <p>
 * The detector follows the Jackson serializer registered for the type: scalar serializers
 * ({@link com.fasterxml.jackson.databind.ser.std.StdScalarSerializer},
 * {@link com.fasterxml.jackson.databind.ser.std.ToStringSerializer},
 * {@link com.fasterxml.jackson.databind.ser.std.NumberSerializer}, etc.) mark the type as
 * simple, while bean and container serializers mark it as structured.
 * <p>
 * This makes Jackson the single source of truth for serialization, diff and XML unwrapping:
 * if a type has to be treated as a value, a scalar serializer has to be registered for it.
 * The detector does not provide separate enable/disable flags.
 *
 * @see JacksonUtils#isStructured(JsonSerializer, SerializerProvider, com.fasterxml.jackson.databind.JavaType)
 */
public class SimpleTypeDetector {
    private final Map<Class<?>, Boolean> cache = new ConcurrentHashMap<>();

    /**
     * Returns whether the given type is a simple (scalar) value.
     *
     * @param type     the type to inspect
     * @param provider the serializer provider of the mapper whose configuration defines the
     *                 serializer; may be {@code null}
     * @return {@code true} if the type is serialized as a scalar, {@code false} otherwise
     */
    public boolean isSimple(Class<?> type, SerializerProvider provider) {
        if (provider == null) {
            return isSimpleFallback(type);
        }
        return cache.computeIfAbsent(type, t -> {
            try {
                JsonSerializer<Object> serializer = provider.findValueSerializer(t);
                return !JacksonUtils.isStructured(serializer, provider, provider.getTypeFactory().constructType(t));
            } catch (Exception e) {
                return isSimpleFallback(t);
            }
        });
    }

    private static boolean isSimpleFallback(Class<?> type) {
        return BeanUtils.isSimpleValueType(type)
                || type == PeriodDuration.class
                || type == byte[].class
                || type == char[].class;
    }
}

package io.github.dimkich.integration.testing.config;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import io.github.dimkich.integration.testing.format.util.JacksonUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects whether a value type is scalar or structured.
 * <p>
 * The detector follows the Jackson serializer registered for the type: scalar serializers
 * ({@link com.fasterxml.jackson.databind.ser.std.StdScalarSerializer},
 * {@link com.fasterxml.jackson.databind.ser.std.ToStringSerializer},
 * {@link com.fasterxml.jackson.databind.ser.std.NumberSerializer}, etc.) mark the type as
 * simple, while bean and container serializers mark the type as structured.
 * <p>
 * This makes Jackson the single source of truth for serialization, diff and XML unwrapping: if a
 * type has to be treated as a value, a scalar serializer has to be registered for it. There is no
 * fallback based on Java types: if Jackson cannot decide, the detection fails instead of silently
 * classifying the value by a rule that may contradict its serializer.
 * <p>
 * {@code byte[]} and {@code char[]} are the only exception: Jackson reports them as arrays in its
 * format visitor, while the diff writes them as scalar text — Base64 for {@code byte[]} and a
 * plain string for {@code char[]}.
 * <p>
 * The detector is used in two contexts: the diff engine, where the provider is taken from the
 * current test format, and XML serialization, where the provider is already available. The
 * answer depends on the serializer configuration, so results are cached per configuration: XML
 * and JSON can run in the same context without one format fixing the classification of the
 * other.
 *
 * @see JacksonUtils#isStructured(JsonSerializer, SerializerProvider, com.fasterxml.jackson.databind.JavaType)
 */
public class SimpleTypeDetector {
    /**
     * Detection results by type and mapper configuration. The answer depends on the serializers
     * registered in the mapper, so every format has its own entry: XML and JSON can run in the
     * same context, and a format switch does not invalidate the other format's results.
     */
    private final ClassValue<Map<SerializationConfig, Boolean>> cache = new ClassValue<>() {
        @Override
        protected Map<SerializationConfig, Boolean> computeValue(@NonNull Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };
    private final ObjectProvider<CompositeTestMapper> testMapper;

    /**
     * The current test mapper, resolved once. The provider is kept lazy so that the detector does
     * not force the format beans to be created before the mappers, but the hot path must not run
     * a Spring lookup for every value.
     */
    private volatile CompositeTestMapper composite;

    /**
     * @param testMapper provider of the current test format, resolved on demand so that the
     *                   detector does not force the format beans to be created before the mappers
     */
    public SimpleTypeDetector(ObjectProvider<CompositeTestMapper> testMapper) {
        this.testMapper = testMapper;
    }

    /**
     * Returns whether the type is serialized as a scalar by the current test format.
     *
     * @param type the type to inspect
     * @return {@code true} if the type is serialized as a scalar
     */
    public boolean isSimple(Class<?> type) {
        return isSimple(type, composite().getSerializerProvider());
    }

    /**
     * Returns whether the given type is a simple (scalar) value.
     * <p>
     * The result is cached by the serialization configuration of the provider, not by the
     * provider instance: Jackson creates a fresh provider per write, while the configuration is
     * stable for a mapper. This keeps the cache bounded by the number of formats and lets XML and
     * JSON share the detector.
     *
     * @param type     the type to inspect
     * @param provider the serializer provider of the mapper whose configuration defines the
     *                 serializer; must not be {@code null}
     * @return {@code true} if the type is serialized as a scalar
     */
    public boolean isSimple(Class<?> type, SerializerProvider provider) {
        Objects.requireNonNull(provider, "SerializerProvider is required to detect a serialized shape");
        SerializationConfig config = provider.getConfig();
        Map<SerializationConfig, Boolean> byConfig = cache.get(type);
        Boolean cached = byConfig.get(config);
        if (cached != null) {
            return cached;
        }
        boolean computed = detect(type, provider);
        Boolean previous = byConfig.putIfAbsent(config, computed);
        return previous == null ? computed : previous;
    }

    private CompositeTestMapper composite() {
        CompositeTestMapper current = composite;
        if (current == null) {
            current = testMapper.getObject();
            composite = current;
        }
        return current;
    }

    private boolean detect(Class<?> type, SerializerProvider provider) {
        if (type == byte[].class || type == char[].class) {
            return true;
        }
        try {
            JsonSerializer<Object> serializer = provider.findValueSerializer(type);
            return !JacksonUtils.isStructured(serializer, provider, provider.getTypeFactory().constructType(type));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot determine the serialized shape of type "
                    + type.getName() + ": " + e.getMessage(), e);
        }
    }
}

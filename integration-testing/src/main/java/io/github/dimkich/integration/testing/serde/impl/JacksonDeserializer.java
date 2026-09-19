package io.github.dimkich.integration.testing.serde.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;
import lombok.SneakyThrows;

import java.lang.reflect.Type;

/**
 * Deserializes bytes to the target type with a Jackson {@link ObjectMapper}.
 *
 * @param <T> the value type
 */
public class JacksonDeserializer<T> implements TestSerdeDeserializer<T, SerdeContext> {
    private final ObjectMapper objectMapper;
    private final JavaType javaType;

    /**
     * Creates the deserializer.
     *
     * @param objectMapper the mapper to read values with
     * @param targetType the type to deserialize to; when {@code null},
     *                   {@link Object} is used
     */
    public JacksonDeserializer(ObjectMapper objectMapper, Type targetType) {
        this.objectMapper = objectMapper;
        this.javaType = targetType != null
                ? objectMapper.constructType(targetType)
                : objectMapper.constructType(Object.class);
    }

    @Override
    public Class<SerdeContext> getContextClass() {
        return SerdeContext.class;
    }

    @Override
    @SneakyThrows
    public T deserialize(byte[] data, SerdeContext context) {
        if (data == null) {
            return null;
        }
        return objectMapper.readValue(data, javaType);
    }
}

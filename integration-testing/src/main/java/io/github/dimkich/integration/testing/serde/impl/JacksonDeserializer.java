package io.github.dimkich.integration.testing.serde.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Type;

/**
 * Converter that deserializes bytes to the target type with a Jackson {@link ObjectMapper}.
 */
@Getter
public class JacksonDeserializer implements TestSerdeConverter<byte[], Object, TestSerdeContext> {

    @Getter(AccessLevel.NONE)
    private final ObjectMapper objectMapper;

    @Getter(AccessLevel.NONE)
    private final JavaType javaType;

    private final Class<byte[]> inputClass = byte[].class;

    private final Class<Object> outputClass = Object.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    /**
     * Creates the converter.
     *
     * @param objectMapper the mapper to read values with
     * @param targetType   the type to deserialize to; when {@code null}, {@link Object} is used
     */
    public JacksonDeserializer(ObjectMapper objectMapper, Type targetType) {
        this.objectMapper = objectMapper;
        this.javaType = objectMapper.constructType(targetType != null ? targetType : Object.class);
    }

    @Override
    @SneakyThrows
    public Object convert(byte[] input, TestSerdeContext context) {
        return input == null ? null : objectMapper.readValue(input, javaType);
    }
}

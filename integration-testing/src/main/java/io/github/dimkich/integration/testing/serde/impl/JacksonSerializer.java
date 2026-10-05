package io.github.dimkich.integration.testing.serde.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * Converter that serializes values to bytes with a Jackson {@link ObjectMapper}.
 */
@Getter
@RequiredArgsConstructor
public class JacksonSerializer implements TestSerdeConverter<Object, byte[], TestSerdeContext> {

    private final ObjectMapper objectMapper;

    private final Class<Object> inputClass = Object.class;

    private final Class<byte[]> outputClass = byte[].class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    @Override
    @SneakyThrows
    public byte[] convert(Object input, TestSerdeContext context) {
        return input == null ? null : objectMapper.writeValueAsBytes(input);
    }
}

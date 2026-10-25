package io.github.dimkich.integration.testing.serde.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.nio.ByteBuffer;

/**
 * Converter that serializes values to a {@link ByteBuffer} with a Jackson {@link ObjectMapper}.
 */
@Getter
@RequiredArgsConstructor
public class JacksonSerializer implements TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> {

    private final ObjectMapper objectMapper;

    private final Class<Object> inputClass = Object.class;

    private final Class<ByteBuffer> outputClass = ByteBuffer.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    @Override
    @SneakyThrows
    public ByteBuffer convert(Object input, TestSerdeContext context) {
        return input == null ? null : TestSerdeConverter.wrap(objectMapper.writeValueAsBytes(input));
    }
}

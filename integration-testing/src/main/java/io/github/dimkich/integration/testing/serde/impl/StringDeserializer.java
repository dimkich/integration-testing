package io.github.dimkich.integration.testing.serde.impl;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Converter that decodes a UTF-8 {@link ByteBuffer} to a string without copying the bytes.
 */
@Getter
public class StringDeserializer implements TestSerdeConverter<ByteBuffer, Object, TestSerdeContext> {

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;

    private final Class<Object> outputClass = Object.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    @Override
    public Object convert(ByteBuffer input, TestSerdeContext context) {
        return input == null ? null : StandardCharsets.UTF_8.decode(input.duplicate()).toString();
    }
}

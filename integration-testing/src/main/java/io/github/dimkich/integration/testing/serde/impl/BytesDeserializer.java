package io.github.dimkich.integration.testing.serde.impl;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;

import java.nio.ByteBuffer;

/**
 * Converter of the {@code bytes} format: returns the remaining bytes of the binary value as a
 * defensive copy.
 */
@Getter
public class BytesDeserializer implements TestSerdeConverter<ByteBuffer, Object, TestSerdeContext> {

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;

    private final Class<Object> outputClass = Object.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    @Override
    public Object convert(ByteBuffer input, TestSerdeContext context) {
        return TestSerdeConverter.toByteArray(input);
    }
}

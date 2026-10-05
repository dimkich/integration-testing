package io.github.dimkich.integration.testing.serde.impl;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

/**
 * Converter that decodes UTF-8 bytes to a string.
 */
@Getter
public class StringDeserializer implements TestSerdeConverter<byte[], Object, TestSerdeContext> {

    private final Class<byte[]> inputClass = byte[].class;

    private final Class<Object> outputClass = Object.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    @Override
    public Object convert(byte[] input, TestSerdeContext context) {
        return input == null ? null : new String(input, StandardCharsets.UTF_8);
    }
}

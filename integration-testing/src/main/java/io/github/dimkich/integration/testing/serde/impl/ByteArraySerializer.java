package io.github.dimkich.integration.testing.serde.impl;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;

/**
 * Converter that casts the input to a byte array and returns it unchanged.
 */
@Getter
public class ByteArraySerializer implements TestSerdeConverter<Object, byte[], TestSerdeContext> {

    private final Class<Object> inputClass = Object.class;

    private final Class<byte[]> outputClass = byte[].class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    @Override
    public byte[] convert(Object input, TestSerdeContext context) {
        return (byte[]) input;
    }
}

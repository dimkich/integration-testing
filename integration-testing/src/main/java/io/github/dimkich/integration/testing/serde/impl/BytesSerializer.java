package io.github.dimkich.integration.testing.serde.impl;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;

import java.nio.ByteBuffer;

/**
 * Converter of the {@code bytes} format: wraps a raw byte value into a read-ready
 * {@link ByteBuffer}. Accepts a byte array as the domain value and a {@link ByteBuffer} as an
 * already serialized value.
 */
@Getter
public class BytesSerializer implements TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> {

    private final Class<Object> inputClass = Object.class;

    private final Class<ByteBuffer> outputClass = ByteBuffer.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    @Override
    public ByteBuffer convert(Object input, TestSerdeContext context) {
        if (input == null) {
            return null;
        }
        if (input instanceof byte[] bytes) {
            return TestSerdeConverter.wrap(bytes);
        }
        if (input instanceof ByteBuffer buffer) {
            return buffer.slice();
        }
        throw new IllegalArgumentException("Expected byte[] or ByteBuffer, but got "
                + input.getClass().getName());
    }
}

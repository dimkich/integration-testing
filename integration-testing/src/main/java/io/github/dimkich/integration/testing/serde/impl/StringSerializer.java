package io.github.dimkich.integration.testing.serde.impl;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Converter that encodes a value to a UTF-8 {@link ByteBuffer} via {@code String.valueOf}.
 */
@Getter
public class StringSerializer implements TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> {

    private final Class<Object> inputClass = Object.class;

    private final Class<ByteBuffer> outputClass = ByteBuffer.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    @Override
    public ByteBuffer convert(Object input, TestSerdeContext context) {
        return input == null ? null
                : TestSerdeConverter.wrap(String.valueOf(input).getBytes(StandardCharsets.UTF_8));
    }
}

package io.github.dimkich.integration.testing.serde.impl;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

/**
 * Converter that encodes a value to UTF-8 bytes via {@code String.valueOf}.
 */
@Getter
public class StringSerializer implements TestSerdeConverter<Object, byte[], TestSerdeContext> {

    private final Class<Object> inputClass = Object.class;

    private final Class<byte[]> outputClass = byte[].class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    @Override
    public byte[] convert(Object input, TestSerdeContext context) {
        return input == null ? null : String.valueOf(input).getBytes(StandardCharsets.UTF_8);
    }
}

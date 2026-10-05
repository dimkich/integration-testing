package io.github.dimkich.integration.testing.serde.provider;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

@Getter
public class TaggedSerializer implements TestSerdeConverter<Object, byte[], TestSerdeContext> {

    private final Class<Object> inputClass = Object.class;
    private final Class<byte[]> outputClass = byte[].class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final String prefix;

    public TaggedSerializer(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public byte[] convert(Object input, TestSerdeContext context) {
        return input == null ? null : (prefix + ":" + input).getBytes(StandardCharsets.UTF_8);
    }
}

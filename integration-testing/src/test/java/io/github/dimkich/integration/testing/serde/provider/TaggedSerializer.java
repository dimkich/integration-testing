package io.github.dimkich.integration.testing.serde.provider;

import java.nio.ByteBuffer;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

@Getter
public class TaggedSerializer implements TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> {

    private final Class<Object> inputClass = Object.class;
    private final Class<ByteBuffer> outputClass = ByteBuffer.class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final String prefix;

    public TaggedSerializer(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public ByteBuffer convert(Object input, TestSerdeContext context) {
        return input == null ? null
                : TestSerdeConverter.wrap((prefix + ":" + input).getBytes(StandardCharsets.UTF_8));
    }
}

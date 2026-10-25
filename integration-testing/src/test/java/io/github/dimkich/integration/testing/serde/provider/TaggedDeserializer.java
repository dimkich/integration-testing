package io.github.dimkich.integration.testing.serde.provider;

import java.nio.ByteBuffer;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

@Getter
public class TaggedDeserializer implements TestSerdeConverter<ByteBuffer, Object, TestSerdeContext> {

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;
    private final Class<Object> outputClass = Object.class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final String prefix;

    public TaggedDeserializer(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Object convert(ByteBuffer input, TestSerdeContext context) {
        if (input == null) {
            return null;
        }
        String text = StandardCharsets.UTF_8.decode(input.duplicate()).toString();
        String tag = prefix + ":";
        return text.startsWith(tag) ? text.substring(tag.length()) : text;
    }
}

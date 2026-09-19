package io.github.dimkich.integration.testing.serde.provider;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class TaggedDeserializer implements TestSerdeDeserializer<Object, SerdeContext> {
    private final String prefix;

    @Override
    public Class<SerdeContext> getContextClass() {
        return SerdeContext.class;
    }

    @Override
    public Object deserialize(byte[] data, SerdeContext context) {
        if (data == null) {
            return null;
        }
        String text = new String(data, StandardCharsets.UTF_8);
        String tag = prefix + ":";
        return text.startsWith(tag) ? text.substring(tag.length()) : text;
    }
}

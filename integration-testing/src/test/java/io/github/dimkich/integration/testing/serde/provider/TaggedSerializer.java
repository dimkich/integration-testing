package io.github.dimkich.integration.testing.serde.provider;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class TaggedSerializer implements TestSerdeSerializer<Object, SerdeContext> {
    private final String prefix;

    @Override
    public Class<SerdeContext> getContextClass() {
        return SerdeContext.class;
    }

    @Override
    public byte[] serialize(Object data, SerdeContext context) {
        return data == null ? null : (prefix + ":" + data).getBytes(StandardCharsets.UTF_8);
    }
}

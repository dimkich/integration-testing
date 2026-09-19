package io.github.dimkich.integration.testing.serde.impl;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;

import java.nio.charset.StandardCharsets;

/**
 * Serializes strings to UTF-8 bytes.
 */
public class StringSerializer implements TestSerdeSerializer<String, SerdeContext> {
    @Override
    public Class<SerdeContext> getContextClass() {
        return SerdeContext.class;
    }

    @Override
    public byte[] serialize(String data, SerdeContext context) {
        return data == null ? null : data.getBytes(StandardCharsets.UTF_8);
    }
}

package io.github.dimkich.integration.testing.serde.impl;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;

import java.nio.charset.StandardCharsets;

/**
 * Deserializes UTF-8 bytes to a string.
 */
public class StringDeserializer implements TestSerdeDeserializer<String, SerdeContext> {
    @Override
    public Class<SerdeContext> getContextClass() {
        return SerdeContext.class;
    }

    @Override
    public String deserialize(byte[] data, SerdeContext context) {
        return data == null ? null : new String(data, StandardCharsets.UTF_8);
    }
}

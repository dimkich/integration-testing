package io.github.dimkich.integration.testing.serde.impl;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;

/**
 * Pass-through deserializer: returns the byte array unchanged.
 */
public class ByteArrayDeserializer implements TestSerdeDeserializer<byte[], SerdeContext> {
    @Override
    public Class<SerdeContext> getContextClass() {
        return SerdeContext.class;
    }

    @Override
    public byte[] deserialize(byte[] data, SerdeContext context) {
        return data;
    }
}

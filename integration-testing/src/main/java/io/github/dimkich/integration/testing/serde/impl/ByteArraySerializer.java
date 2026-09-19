package io.github.dimkich.integration.testing.serde.impl;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;

/**
 * Pass-through serializer: returns the byte array unchanged.
 */
public class ByteArraySerializer implements TestSerdeSerializer<byte[], SerdeContext> {

    @Override
    public Class<SerdeContext> getContextClass() {
        return SerdeContext.class;
    }

    @Override
    public byte[] serialize(byte[] data, SerdeContext context) {
        return data;
    }
}

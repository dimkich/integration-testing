package io.github.dimkich.integration.testing.serde.providers;

import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeProvider;
import io.github.dimkich.integration.testing.serde.impl.ByteArrayDeserializer;
import io.github.dimkich.integration.testing.serde.impl.ByteArraySerializer;

/**
 * Pass-through provider registered under the name {@code bytes}: both serialization
 * and deserialization return the payload unchanged.
 */
public class ByteArraySerdeProvider implements TestSerdeProvider<SerdeProperties> {
    @Override
    public String getName() {
        return "bytes";
    }

    @Override
    public Object createSerializer(SerdeProperties config) {
        return new ByteArraySerializer();
    }

    @Override
    public Object createDeserializer(SerdeProperties config) {
        return new ByteArrayDeserializer();
    }
}

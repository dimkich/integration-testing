package io.github.dimkich.integration.testing.serde.providers;

import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeProvider;
import io.github.dimkich.integration.testing.serde.impl.StringDeserializer;
import io.github.dimkich.integration.testing.serde.impl.StringSerializer;

/**
 * UTF-8 string provider registered under the name {@code string}.
 */
public class StringSerdeProvider implements TestSerdeProvider<SerdeProperties> {
    @Override
    public String getName() {
        return "string";
    }

    @Override
    public Object createSerializer(SerdeProperties config) {
        return new StringSerializer();
    }

    @Override
    public Object createDeserializer(SerdeProperties config) {
        return new StringDeserializer();
    }
}

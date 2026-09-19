package io.github.dimkich.integration.testing.serde.provider;

import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeProvider;

public class TaggedSerdeProvider implements TestSerdeProvider<SerdeProperties> {
    @Override
    public String getName() {
        return "tagged";
    }

    @Override
    public Object createSerializer(SerdeProperties config) {
        return new TaggedSerializer("TAG");
    }

    @Override
    public Object createDeserializer(SerdeProperties config) {
        return new TaggedDeserializer("TAG");
    }
}

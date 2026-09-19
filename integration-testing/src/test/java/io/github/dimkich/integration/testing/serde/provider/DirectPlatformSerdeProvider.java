package io.github.dimkich.integration.testing.serde.provider;

import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeProvider;
import io.github.dimkich.integration.testing.serde.platform.DirectPlatformDeserializer;
import io.github.dimkich.integration.testing.serde.platform.DirectPlatformSerializer;

public class DirectPlatformSerdeProvider implements TestSerdeProvider<SerdeProperties> {
    @Override
    public String getName() {
        return "platform-direct";
    }

    @Override
    public Object createSerializer(SerdeProperties config) {
        return new DirectPlatformSerializer();
    }

    @Override
    public Object createDeserializer(SerdeProperties config) {
        return new DirectPlatformDeserializer();
    }
}

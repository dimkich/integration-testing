package io.github.dimkich.integration.testing.serde.platform.adapter;

import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.platform.DefaultPlatformRecordDeserializer;
import io.github.dimkich.integration.testing.serde.platform.PlatformDeserializer;
import io.github.dimkich.integration.testing.serde.platform.PlatformRecordDeserializer;
import io.github.dimkich.integration.testing.serde.platform.TestRecordProperties;

public class PlatformDeserializerToPlatformRecordDeserializerAdapter implements TestSerdeAdapter<
        PlatformDeserializer,
        PlatformRecordDeserializer,
        TestRecordProperties> {

    @Override
    public PlatformRecordDeserializer adapt(PlatformDeserializer source, TestRecordProperties properties) {
        return new DefaultPlatformRecordDeserializer(source);
    }
}

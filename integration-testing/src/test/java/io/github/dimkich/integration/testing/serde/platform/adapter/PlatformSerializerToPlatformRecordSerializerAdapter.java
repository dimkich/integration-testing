package io.github.dimkich.integration.testing.serde.platform.adapter;

import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.platform.DefaultPlatformRecordSerializer;
import io.github.dimkich.integration.testing.serde.platform.PlatformRecordSerializer;
import io.github.dimkich.integration.testing.serde.platform.PlatformSerializer;
import io.github.dimkich.integration.testing.serde.platform.TestRecordProperties;

public class PlatformSerializerToPlatformRecordSerializerAdapter implements TestSerdeAdapter<
        PlatformSerializer,
        PlatformRecordSerializer,
        TestRecordProperties> {

    @Override
    public PlatformRecordSerializer adapt(PlatformSerializer source, TestRecordProperties properties) {
        return new DefaultPlatformRecordSerializer(source, properties.getKeyPrefix(), properties.getHeaderName());
    }
}

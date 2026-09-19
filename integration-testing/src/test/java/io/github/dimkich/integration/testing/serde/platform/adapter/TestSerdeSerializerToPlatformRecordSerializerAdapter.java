package io.github.dimkich.integration.testing.serde.platform.adapter;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import io.github.dimkich.integration.testing.serde.platform.PlatformRecordSerializer;
import io.github.dimkich.integration.testing.serde.platform.TestRecordProperties;

public class TestSerdeSerializerToPlatformRecordSerializerAdapter implements TestSerdeAdapter<
        TestSerdeSerializer<Object, ? extends SerdeContext>,
        PlatformRecordSerializer,
        TestRecordProperties> {

    private final CoreToPlatformSerializerAdapter coreToPlatform = new CoreToPlatformSerializerAdapter();
    private final PlatformSerializerToPlatformRecordSerializerAdapter serializerToRecord =
            new PlatformSerializerToPlatformRecordSerializerAdapter();

    @Override
    public PlatformRecordSerializer adapt(TestSerdeSerializer<Object, ? extends SerdeContext> source,
                                          TestRecordProperties properties) {
        return serializerToRecord.adapt(coreToPlatform.adapt(source, properties), properties);
    }
}

package io.github.dimkich.integration.testing.serde.platform.adapter;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;
import io.github.dimkich.integration.testing.serde.platform.PlatformRecordDeserializer;
import io.github.dimkich.integration.testing.serde.platform.TestRecordProperties;

public class TestSerdeDeserializerToPlatformRecordDeserializerAdapter implements TestSerdeAdapter<
        TestSerdeDeserializer<Object, ? extends SerdeContext>,
        PlatformRecordDeserializer,
        TestRecordProperties> {

    private final CoreToPlatformDeserializerAdapter coreToPlatform = new CoreToPlatformDeserializerAdapter();
    private final PlatformDeserializerToPlatformRecordDeserializerAdapter deserializerToRecord =
            new PlatformDeserializerToPlatformRecordDeserializerAdapter();

    @Override
    public PlatformRecordDeserializer adapt(TestSerdeDeserializer<Object, ? extends SerdeContext> source,
                                            TestRecordProperties properties) {
        return deserializerToRecord.adapt(coreToPlatform.adapt(source, properties), properties);
    }
}

package io.github.dimkich.integration.testing.serde.platform.adapter;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;
import io.github.dimkich.integration.testing.serde.platform.DefaultPlatformDeserializer;
import io.github.dimkich.integration.testing.serde.platform.PlatformDeserializer;
import io.github.dimkich.integration.testing.serde.platform.TestPlatformContext;
import io.github.dimkich.integration.testing.serde.platform.TestRecordProperties;

public class SpecificConfigCoreToPlatformDeserializerAdapter implements TestSerdeAdapter<
        TestSerdeDeserializer<Object, ? extends SerdeContext>,
        PlatformDeserializer,
        TestRecordProperties> {

    @Override
    public PlatformDeserializer adapt(TestSerdeDeserializer<Object, ? extends SerdeContext> source,
                                      TestRecordProperties properties) {
        source.assertAcceptsContext(TestPlatformContext.class, CoreToPlatformDeserializerAdapter.CONTEXT_HINT);
        return new DefaultPlatformDeserializer(source, "S:");
    }
}

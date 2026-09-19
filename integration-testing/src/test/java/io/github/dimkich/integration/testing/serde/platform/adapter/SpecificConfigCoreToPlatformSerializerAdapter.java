package io.github.dimkich.integration.testing.serde.platform.adapter;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import io.github.dimkich.integration.testing.serde.platform.DefaultPlatformSerializer;
import io.github.dimkich.integration.testing.serde.platform.PlatformSerializer;
import io.github.dimkich.integration.testing.serde.platform.TestPlatformContext;
import io.github.dimkich.integration.testing.serde.platform.TestRecordProperties;

public class SpecificConfigCoreToPlatformSerializerAdapter implements TestSerdeAdapter<
        TestSerdeSerializer<Object, ? extends SerdeContext>,
        PlatformSerializer,
        TestRecordProperties> {

    @Override
    public PlatformSerializer adapt(TestSerdeSerializer<Object, ? extends SerdeContext> source,
                                    TestRecordProperties properties) {
        source.assertAcceptsContext(TestPlatformContext.class, CoreToPlatformSerializerAdapter.CONTEXT_HINT);
        return new DefaultPlatformSerializer(source, "S:");
    }
}

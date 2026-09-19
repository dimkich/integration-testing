package io.github.dimkich.integration.testing.serde.platform.adapter;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import io.github.dimkich.integration.testing.serde.platform.DefaultPlatformSerializer;
import io.github.dimkich.integration.testing.serde.platform.PlatformSerializer;
import io.github.dimkich.integration.testing.serde.platform.TestPlatformContext;

public class CoreToPlatformSerializerAdapter implements TestSerdeAdapter<
        TestSerdeSerializer<Object, ? extends SerdeContext>,
        PlatformSerializer,
        SerdeProperties> {
    public static final String CONTEXT_HINT =
            "Use a provider that accepts TestPlatformContext or work with the core TestSerdeSerializer directly.";

    private final String marker;

    public CoreToPlatformSerializerAdapter() {
        this("B:");
    }

    protected CoreToPlatformSerializerAdapter(String marker) {
        this.marker = marker;
    }

    @Override
    public PlatformSerializer adapt(TestSerdeSerializer<Object, ? extends SerdeContext> source,
                                    SerdeProperties properties) {
        source.assertAcceptsContext(TestPlatformContext.class, CONTEXT_HINT);
        return new DefaultPlatformSerializer(source, marker);
    }
}

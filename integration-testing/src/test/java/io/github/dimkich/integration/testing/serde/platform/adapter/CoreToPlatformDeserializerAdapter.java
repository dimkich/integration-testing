package io.github.dimkich.integration.testing.serde.platform.adapter;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;
import io.github.dimkich.integration.testing.serde.platform.DefaultPlatformDeserializer;
import io.github.dimkich.integration.testing.serde.platform.PlatformDeserializer;
import io.github.dimkich.integration.testing.serde.platform.TestPlatformContext;

public class CoreToPlatformDeserializerAdapter implements TestSerdeAdapter<
        TestSerdeDeserializer<Object, ? extends SerdeContext>,
        PlatformDeserializer,
        SerdeProperties> {
    public static final String CONTEXT_HINT =
            "Use a provider that accepts TestPlatformContext or work with the core TestSerdeDeserializer directly.";

    private final String marker;

    public CoreToPlatformDeserializerAdapter() {
        this("B:");
    }

    protected CoreToPlatformDeserializerAdapter(String marker) {
        this.marker = marker;
    }

    @Override
    public PlatformDeserializer adapt(TestSerdeDeserializer<Object, ? extends SerdeContext> source,
                                      SerdeProperties properties) {
        source.assertAcceptsContext(TestPlatformContext.class, CONTEXT_HINT);
        return new DefaultPlatformDeserializer(source, marker);
    }
}

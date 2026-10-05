package io.github.dimkich.integration.testing.serde.provider;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeConverterFactory;
import io.github.dimkich.integration.testing.serde.platform.PlainConfig;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class PlainConfigTaggedSerializerFactory
        implements TestSerdeConverterFactory<Object, byte[], TestSerdeContext, ComponentRole, PlainConfig> {

    private final Class<Object> inputClass = Object.class;

    private final Class<byte[]> outputClass = byte[].class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<PlainConfig> propertiesClass = PlainConfig.class;

    @Override
    @Nullable
    public TestSerdeConverter<Object, byte[], TestSerdeContext> create(PlainConfig config, Class<Object> inputClass,
                                                                       Class<byte[]> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        if ("tagged".equals(config.getFormat())) {
            return new TaggedSerializer("PLAIN");
        }
        return null;
    }
}

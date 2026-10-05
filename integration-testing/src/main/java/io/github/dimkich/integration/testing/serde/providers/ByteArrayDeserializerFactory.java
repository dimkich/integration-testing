package io.github.dimkich.integration.testing.serde.providers;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProviderFactory;
import io.github.dimkich.integration.testing.serde.impl.ByteArrayDeserializer;
import io.github.dimkich.integration.testing.serde.properties.ProviderProperties;
import lombok.Getter;
import org.springframework.lang.Nullable;

/**
 * Provider {@code bytes}: deserializer that returns the byte array unchanged.
 */
@Getter
public class ByteArrayDeserializerFactory
        implements TestSerdeProviderFactory<byte[], Object, TestSerdeContext, ComponentRole, ProviderProperties> {

    private final Class<byte[]> inputClass = byte[].class;

    private final Class<Object> outputClass = Object.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<ProviderProperties> propertiesClass = ProviderProperties.class;

    private final String name = "bytes";

    @Override
    public TestSerdeConverter<byte[], Object, TestSerdeContext> create(ProviderProperties config, Class<byte[]> inputClass,
                                                                       Class<Object> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        return new ByteArrayDeserializer();
    }
}

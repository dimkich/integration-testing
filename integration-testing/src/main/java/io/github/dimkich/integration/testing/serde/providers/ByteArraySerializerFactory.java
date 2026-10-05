package io.github.dimkich.integration.testing.serde.providers;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProviderFactory;
import io.github.dimkich.integration.testing.serde.impl.ByteArraySerializer;
import io.github.dimkich.integration.testing.serde.properties.ProviderProperties;
import lombok.Getter;
import org.springframework.lang.Nullable;

/**
 * Provider {@code bytes}: serializer that returns the byte array unchanged.
 */
@Getter
public class ByteArraySerializerFactory
        implements TestSerdeProviderFactory<Object, byte[], TestSerdeContext, ComponentRole, ProviderProperties> {

    private final Class<Object> inputClass = Object.class;

    private final Class<byte[]> outputClass = byte[].class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<ProviderProperties> propertiesClass = ProviderProperties.class;

    private final String name = "bytes";

    @Override
    public TestSerdeConverter<Object, byte[], TestSerdeContext> create(ProviderProperties config, Class<Object> inputClass,
                                                                       Class<byte[]> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        return new ByteArraySerializer();
    }
}

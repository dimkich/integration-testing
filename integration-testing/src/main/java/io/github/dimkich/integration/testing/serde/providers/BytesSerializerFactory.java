package io.github.dimkich.integration.testing.serde.providers;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProviderFactory;
import io.github.dimkich.integration.testing.serde.impl.BytesSerializer;
import io.github.dimkich.integration.testing.serde.properties.ProviderProperties;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;

/**
 * Provider {@code bytes}: serializer that wraps a raw byte value into a {@link ByteBuffer}.
 */
@Getter
public class BytesSerializerFactory
        implements TestSerdeProviderFactory<Object, ByteBuffer, TestSerdeContext, ComponentRole,
        ProviderProperties> {

    private final Class<Object> inputClass = Object.class;

    private final Class<ByteBuffer> outputClass = ByteBuffer.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<ProviderProperties> propertiesClass = ProviderProperties.class;

    private final String name = "bytes";

    @Override
    public TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> create(
            ProviderProperties config, Class<Object> inputClass, Class<ByteBuffer> outputClass,
            Class<TestSerdeContext> contextClass, @Nullable ComponentRole role) {
        return new BytesSerializer();
    }
}

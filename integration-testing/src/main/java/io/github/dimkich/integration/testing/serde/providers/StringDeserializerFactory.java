package io.github.dimkich.integration.testing.serde.providers;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProviderFactory;
import io.github.dimkich.integration.testing.serde.impl.StringDeserializer;
import io.github.dimkich.integration.testing.serde.properties.ProviderProperties;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;

/**
 * Provider {@code string}: deserializes a UTF-8 {@link ByteBuffer} to a string.
 */
@Getter
public class StringDeserializerFactory
        implements TestSerdeProviderFactory<ByteBuffer, Object, TestSerdeContext, ComponentRole,
        ProviderProperties> {

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;

    private final Class<Object> outputClass = Object.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<ProviderProperties> propertiesClass = ProviderProperties.class;

    private final String name = "string";

    @Override
    public TestSerdeConverter<ByteBuffer, Object, TestSerdeContext> create(
            ProviderProperties config, Class<ByteBuffer> inputClass, Class<Object> outputClass,
            Class<TestSerdeContext> contextClass, @Nullable ComponentRole role) {
        return new StringDeserializer();
    }
}

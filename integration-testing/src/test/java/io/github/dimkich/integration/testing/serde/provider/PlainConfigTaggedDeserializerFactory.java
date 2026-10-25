package io.github.dimkich.integration.testing.serde.provider;

import java.nio.ByteBuffer;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeConverterFactory;
import io.github.dimkich.integration.testing.serde.platform.PlainConfig;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class PlainConfigTaggedDeserializerFactory
        implements TestSerdeConverterFactory<ByteBuffer, Object, TestSerdeContext, ComponentRole, PlainConfig> {

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;

    private final Class<Object> outputClass = Object.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<PlainConfig> propertiesClass = PlainConfig.class;

    @Override
    @Nullable
    public TestSerdeConverter<ByteBuffer, Object, TestSerdeContext> create(PlainConfig config, Class<ByteBuffer> inputClass,
                                                                       Class<Object> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        if ("tagged".equals(config.getFormat())) {
            return new TaggedDeserializer("PLAIN");
        }
        return null;
    }
}

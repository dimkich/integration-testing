package io.github.dimkich.integration.testing.serde.provider;

import java.nio.ByteBuffer;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeConverterFactory;
import io.github.dimkich.integration.testing.serde.impl.BytesSerializer;
import io.github.dimkich.integration.testing.serde.platform.PlainConfig;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class PlainConfigBytesSerializerFactory
        implements TestSerdeConverterFactory<Object, ByteBuffer, TestSerdeContext, ComponentRole, PlainConfig> {

    private final Class<Object> inputClass = Object.class;

    private final Class<ByteBuffer> outputClass = ByteBuffer.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<PlainConfig> propertiesClass = PlainConfig.class;

    @Override
    @Nullable
    public TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> create(PlainConfig config, Class<Object> inputClass,
                                                                       Class<ByteBuffer> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        if ("bytes".equals(config.getFormat())) {
            return new BytesSerializer();
        }
        return null;
    }
}

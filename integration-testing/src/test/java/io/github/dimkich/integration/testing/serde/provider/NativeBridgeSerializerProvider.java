package io.github.dimkich.integration.testing.serde.provider;

import java.nio.ByteBuffer;

import io.github.dimkich.integration.testing.serde.*;
import io.github.dimkich.integration.testing.serde.platform.NativeBridgeSerializer;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class NativeBridgeSerializerProvider
        implements TestSerdeProviderFactory<Object, ByteBuffer, TestSerdeContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<Object> inputClass = Object.class;
    private final Class<ByteBuffer> outputClass = ByteBuffer.class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "native-bridge";

    @Override
    public TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> create(StandardSerdeProperties config,
                                                                       Class<Object> inputClass,
                                                                       Class<ByteBuffer> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        NativeBridgeSerializer source = new NativeBridgeSerializer();
        return TestSerdeConverter.of(Object.class, ByteBuffer.class, TestSerdeContext.class,
                (input, context) -> TestSerdeConverter.wrap(source.serialize(input)));
    }
}

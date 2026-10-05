package io.github.dimkich.integration.testing.serde.provider;

import io.github.dimkich.integration.testing.serde.*;
import io.github.dimkich.integration.testing.serde.platform.NativeBridgeDeserializer;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class NativeBridgeDeserializerProvider
        implements TestSerdeProviderFactory<byte[], Object, TestSerdeContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<byte[]> inputClass = byte[].class;
    private final Class<Object> outputClass = Object.class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "native-bridge";

    @Override
    public TestSerdeConverter<byte[], Object, TestSerdeContext> create(StandardSerdeProperties config,
                                                                       Class<byte[]> inputClass,
                                                                       Class<Object> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        NativeBridgeDeserializer source = new NativeBridgeDeserializer();
        return TestSerdeConverter.of(byte[].class, Object.class, TestSerdeContext.class,
                (input, context) -> source.deserialize(input));
    }
}

package io.github.dimkich.integration.testing.serde.platform.adapter;

import io.github.dimkich.integration.testing.serde.*;
import io.github.dimkich.integration.testing.serde.platform.NativeBridgeDeserializer;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class NativeBridgeToCoreDeserializerAdapter implements TestSerdeAdapter<
        NativeBridgeDeserializer,
        byte[],
        Object,
        TestSerdeContext,
        ComponentRole,
        StandardSerdeProperties> {

    private final Class<NativeBridgeDeserializer> sourceClass = NativeBridgeDeserializer.class;

    private final Class<byte[]> inputClass = byte[].class;

    private final Class<Object> outputClass = Object.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<byte[], Object, TestSerdeContext> adapt(
            NativeBridgeDeserializer source, StandardSerdeProperties properties, Class<byte[]> inputClass,
            Class<Object> outputClass, Class<TestSerdeContext> contextClass, @Nullable ComponentRole role) {
        if (!byte[].class.equals(inputClass)) {
            return null;
        }
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> source.deserialize(input));
    }
}

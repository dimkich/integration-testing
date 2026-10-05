package io.github.dimkich.integration.testing.serde.platform.adapter;

import io.github.dimkich.integration.testing.serde.*;
import io.github.dimkich.integration.testing.serde.platform.NativeBridgeSerializer;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class NativeBridgeToCoreSerializerAdapter implements TestSerdeAdapter<
        NativeBridgeSerializer,
        Object,
        byte[],
        TestSerdeContext,
        ComponentRole,
        StandardSerdeProperties> {

    private final Class<NativeBridgeSerializer> sourceClass = NativeBridgeSerializer.class;

    private final Class<Object> inputClass = Object.class;

    private final Class<byte[]> outputClass = byte[].class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<Object, byte[], TestSerdeContext> adapt(
            NativeBridgeSerializer source, StandardSerdeProperties properties, Class<Object> inputClass,
            Class<byte[]> outputClass, Class<TestSerdeContext> contextClass, @Nullable ComponentRole role) {
        if (!byte[].class.equals(outputClass)) {
            return null;
        }
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> source.serialize(input));
    }
}

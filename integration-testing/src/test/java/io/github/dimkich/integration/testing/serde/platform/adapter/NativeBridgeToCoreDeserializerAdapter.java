package io.github.dimkich.integration.testing.serde.platform.adapter;

import java.nio.ByteBuffer;

import io.github.dimkich.integration.testing.serde.*;
import io.github.dimkich.integration.testing.serde.platform.NativeBridgeDeserializer;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class NativeBridgeToCoreDeserializerAdapter implements TestSerdeAdapter<
        NativeBridgeDeserializer,
        ByteBuffer,
        Object,
        TestSerdeContext,
        ComponentRole,
        StandardSerdeProperties> {

    private final Class<NativeBridgeDeserializer> sourceClass = NativeBridgeDeserializer.class;

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;

    private final Class<Object> outputClass = Object.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<ByteBuffer, Object, TestSerdeContext> adapt(
            NativeBridgeDeserializer source, StandardSerdeProperties properties, Class<ByteBuffer> inputClass,
            Class<Object> outputClass, Class<TestSerdeContext> contextClass, @Nullable ComponentRole role) {
        if (!ByteBuffer.class.equals(inputClass)) {
            return null;
        }
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> source.deserialize(TestSerdeConverter.toByteArray(input)));
    }
}

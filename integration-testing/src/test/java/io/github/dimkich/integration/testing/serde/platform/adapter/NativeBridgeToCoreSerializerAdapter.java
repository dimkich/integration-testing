package io.github.dimkich.integration.testing.serde.platform.adapter;

import java.nio.ByteBuffer;

import io.github.dimkich.integration.testing.serde.*;
import io.github.dimkich.integration.testing.serde.platform.NativeBridgeSerializer;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class NativeBridgeToCoreSerializerAdapter implements TestSerdeAdapter<
        NativeBridgeSerializer,
        Object,
        ByteBuffer,
        TestSerdeContext,
        ComponentRole,
        StandardSerdeProperties> {

    private final Class<NativeBridgeSerializer> sourceClass = NativeBridgeSerializer.class;

    private final Class<Object> inputClass = Object.class;

    private final Class<ByteBuffer> outputClass = ByteBuffer.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> adapt(
            NativeBridgeSerializer source, StandardSerdeProperties properties, Class<Object> inputClass,
            Class<ByteBuffer> outputClass, Class<TestSerdeContext> contextClass, @Nullable ComponentRole role) {
        if (!ByteBuffer.class.equals(outputClass)) {
            return null;
        }
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> TestSerdeConverter.wrap(source.serialize(input)));
    }
}

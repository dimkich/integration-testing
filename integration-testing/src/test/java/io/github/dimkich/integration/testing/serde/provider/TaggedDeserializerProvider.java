package io.github.dimkich.integration.testing.serde.provider;

import java.nio.ByteBuffer;

import io.github.dimkich.integration.testing.serde.*;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class TaggedDeserializerProvider
        implements TestSerdeProviderFactory<ByteBuffer, Object, TestSerdeContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;
    private final Class<Object> outputClass = Object.class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "tagged";

    @Override
    public TestSerdeConverter<ByteBuffer, Object, TestSerdeContext> create(StandardSerdeProperties config,
                                                                       Class<ByteBuffer> inputClass,
                                                                       Class<Object> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        return new TaggedDeserializer("TAG");
    }
}

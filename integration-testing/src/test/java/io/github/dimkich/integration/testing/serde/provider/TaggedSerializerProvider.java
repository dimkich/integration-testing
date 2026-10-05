package io.github.dimkich.integration.testing.serde.provider;

import io.github.dimkich.integration.testing.serde.*;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class TaggedSerializerProvider
        implements TestSerdeProviderFactory<Object, byte[], TestSerdeContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<Object> inputClass = Object.class;
    private final Class<byte[]> outputClass = byte[].class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "tagged";

    @Override
    public TestSerdeConverter<Object, byte[], TestSerdeContext> create(StandardSerdeProperties config,
                                                                       Class<Object> inputClass,
                                                                       Class<byte[]> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        return new TaggedSerializer("TAG");
    }
}

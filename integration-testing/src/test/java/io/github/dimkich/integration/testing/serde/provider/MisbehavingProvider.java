package io.github.dimkich.integration.testing.serde.provider;

import java.nio.ByteBuffer;

import io.github.dimkich.integration.testing.serde.*;
import io.github.dimkich.integration.testing.serde.platform.UnrelatedSerdeContext;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class MisbehavingProvider implements TestSerdeProviderFactory<Object, ByteBuffer, TestSerdeContext,
        ComponentRole, StandardSerdeProperties> {

    private final Class<Object> inputClass = Object.class;
    private final Class<ByteBuffer> outputClass = ByteBuffer.class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "misbehaving";

    @Override
    public TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> create(StandardSerdeProperties config,
                                                                       Class<Object> inputClass,
                                                                       Class<ByteBuffer> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        return TestSerdeConverter.uncheckedCast(TestSerdeConverter.of(null, null, UnrelatedSerdeContext.class,
                (input, context) -> input));
    }
}

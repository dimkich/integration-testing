package io.github.dimkich.integration.testing.serde.provider;

import java.nio.ByteBuffer;

import io.github.dimkich.integration.testing.serde.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

@Getter
@RequiredArgsConstructor
public class RoleProvider implements TestSerdeProviderFactory<Object, ByteBuffer, TestSerdeContext, ComponentRole,
        StandardSerdeProperties> {

    private final ComponentRole role;
    private final Class<Object> inputClass = Object.class;
    private final Class<ByteBuffer> outputClass = ByteBuffer.class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "roles";

    @Override
    public TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> create(StandardSerdeProperties config,
                                                                       Class<Object> inputClass,
                                                                       Class<ByteBuffer> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        return null;
    }
}

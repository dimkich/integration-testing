package io.github.dimkich.integration.testing.serde.provider;

import java.nio.ByteBuffer;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProviderFactory;
import io.github.dimkich.integration.testing.serde.platform.UnrelatedSerdeContext;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.nio.charset.StandardCharsets;

@Getter
public class UnrelatedContextSerializerProvider
        implements TestSerdeProviderFactory<Object, ByteBuffer, UnrelatedSerdeContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<Object> inputClass = Object.class;
    private final Class<ByteBuffer> outputClass = ByteBuffer.class;
    private final Class<UnrelatedSerdeContext> contextClass = UnrelatedSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "unrelated-context";

    @Override
    public TestSerdeConverter<Object, ByteBuffer, UnrelatedSerdeContext> create(StandardSerdeProperties config,
                                                                            Class<Object> inputClass,
                                                                            Class<ByteBuffer> outputClass,
                                                                            Class<UnrelatedSerdeContext> contextClass,
                                                                            @Nullable ComponentRole role) {
        return new UnrelatedContextSerializer();
    }

    @Getter
    public static class UnrelatedContextSerializer
            implements TestSerdeConverter<Object, ByteBuffer, UnrelatedSerdeContext> {

        private final Class<Object> inputClass = Object.class;
        private final Class<ByteBuffer> outputClass = ByteBuffer.class;
        private final Class<UnrelatedSerdeContext> contextClass = UnrelatedSerdeContext.class;

        @Override
        public ByteBuffer convert(Object input, UnrelatedSerdeContext context) {
            return input == null ? null
                    : TestSerdeConverter.wrap(("U:" + input).getBytes(StandardCharsets.UTF_8));
        }
    }
}

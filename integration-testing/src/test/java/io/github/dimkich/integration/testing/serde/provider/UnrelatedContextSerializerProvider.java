package io.github.dimkich.integration.testing.serde.provider;

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
        implements TestSerdeProviderFactory<Object, byte[], UnrelatedSerdeContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<Object> inputClass = Object.class;
    private final Class<byte[]> outputClass = byte[].class;
    private final Class<UnrelatedSerdeContext> contextClass = UnrelatedSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "unrelated-context";

    @Override
    public TestSerdeConverter<Object, byte[], UnrelatedSerdeContext> create(StandardSerdeProperties config,
                                                                            Class<Object> inputClass,
                                                                            Class<byte[]> outputClass,
                                                                            Class<UnrelatedSerdeContext> contextClass,
                                                                            @Nullable ComponentRole role) {
        return new UnrelatedContextSerializer();
    }

    @Getter
    public static class UnrelatedContextSerializer
            implements TestSerdeConverter<Object, byte[], UnrelatedSerdeContext> {

        private final Class<Object> inputClass = Object.class;
        private final Class<byte[]> outputClass = byte[].class;
        private final Class<UnrelatedSerdeContext> contextClass = UnrelatedSerdeContext.class;

        @Override
        public byte[] convert(Object input, UnrelatedSerdeContext context) {
            return input == null ? null : ("U:" + input).getBytes(StandardCharsets.UTF_8);
        }
    }
}

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
public class UnrelatedContextDeserializerProvider
        implements TestSerdeProviderFactory<byte[], Object, UnrelatedSerdeContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<byte[]> inputClass = byte[].class;
    private final Class<Object> outputClass = Object.class;
    private final Class<UnrelatedSerdeContext> contextClass = UnrelatedSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "unrelated-context";

    @Override
    public TestSerdeConverter<byte[], Object, UnrelatedSerdeContext> create(StandardSerdeProperties config,
                                                                            Class<byte[]> inputClass,
                                                                            Class<Object> outputClass,
                                                                            Class<UnrelatedSerdeContext> contextClass,
                                                                            @Nullable ComponentRole role) {
        return new UnrelatedContextDeserializer();
    }

    @Getter
    public static class UnrelatedContextDeserializer
            implements TestSerdeConverter<byte[], Object, UnrelatedSerdeContext> {

        private final Class<byte[]> inputClass = byte[].class;
        private final Class<Object> outputClass = Object.class;
        private final Class<UnrelatedSerdeContext> contextClass = UnrelatedSerdeContext.class;

        @Override
        public Object convert(byte[] input, UnrelatedSerdeContext context) {
            if (input == null) {
                return null;
            }
            String text = new String(input, StandardCharsets.UTF_8);
            return text.startsWith("U:") ? text.substring(2) : text;
        }
    }
}

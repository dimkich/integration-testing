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
public class UnrelatedContextDeserializerProvider
        implements TestSerdeProviderFactory<ByteBuffer, Object, UnrelatedSerdeContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;
    private final Class<Object> outputClass = Object.class;
    private final Class<UnrelatedSerdeContext> contextClass = UnrelatedSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "unrelated-context";

    @Override
    public TestSerdeConverter<ByteBuffer, Object, UnrelatedSerdeContext> create(StandardSerdeProperties config,
                                                                            Class<ByteBuffer> inputClass,
                                                                            Class<Object> outputClass,
                                                                            Class<UnrelatedSerdeContext> contextClass,
                                                                            @Nullable ComponentRole role) {
        return new UnrelatedContextDeserializer();
    }

    @Getter
    public static class UnrelatedContextDeserializer
            implements TestSerdeConverter<ByteBuffer, Object, UnrelatedSerdeContext> {

        private final Class<ByteBuffer> inputClass = ByteBuffer.class;
        private final Class<Object> outputClass = Object.class;
        private final Class<UnrelatedSerdeContext> contextClass = UnrelatedSerdeContext.class;

        @Override
        public Object convert(ByteBuffer input, UnrelatedSerdeContext context) {
            if (input == null) {
                return null;
            }
            String text = StandardCharsets.UTF_8.decode(input.duplicate()).toString();
            return text.startsWith("U:") ? text.substring(2) : text;
        }
    }
}

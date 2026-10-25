package io.github.dimkich.integration.testing.serde.provider;

import java.nio.ByteBuffer;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProviderFactory;
import io.github.dimkich.integration.testing.serde.platform.TestPlatformContext;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.nio.charset.StandardCharsets;

@Getter
public class PlatformContextDeserializerProvider
        implements TestSerdeProviderFactory<ByteBuffer, Object, TestPlatformContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;
    private final Class<Object> outputClass = Object.class;
    private final Class<TestPlatformContext> contextClass = TestPlatformContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "platform-context";

    @Override
    public TestSerdeConverter<ByteBuffer, Object, TestPlatformContext> create(StandardSerdeProperties config,
                                                                          Class<ByteBuffer> inputClass,
                                                                          Class<Object> outputClass,
                                                                          Class<TestPlatformContext> contextClass,
                                                                          @Nullable ComponentRole role) {
        return new PlatformContextDeserializer();
    }

    @Getter
    public static class PlatformContextDeserializer implements TestSerdeConverter<ByteBuffer, Object, TestPlatformContext> {

        private final Class<ByteBuffer> inputClass = ByteBuffer.class;
        private final Class<Object> outputClass = Object.class;
        private final Class<TestPlatformContext> contextClass = TestPlatformContext.class;

        @Override
        public Object convert(ByteBuffer input, TestPlatformContext context) {
            if (input == null) {
                return null;
            }
            String text = StandardCharsets.UTF_8.decode(input.duplicate()).toString();
            String tag = context.getChannel() + ":";
            return text.startsWith(tag) ? text.substring(tag.length()) : text;
        }
    }
}

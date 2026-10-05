package io.github.dimkich.integration.testing.serde.provider;

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
        implements TestSerdeProviderFactory<byte[], Object, TestPlatformContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<byte[]> inputClass = byte[].class;
    private final Class<Object> outputClass = Object.class;
    private final Class<TestPlatformContext> contextClass = TestPlatformContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "platform-context";

    @Override
    public TestSerdeConverter<byte[], Object, TestPlatformContext> create(StandardSerdeProperties config,
                                                                          Class<byte[]> inputClass,
                                                                          Class<Object> outputClass,
                                                                          Class<TestPlatformContext> contextClass,
                                                                          @Nullable ComponentRole role) {
        return new PlatformContextDeserializer();
    }

    @Getter
    public static class PlatformContextDeserializer implements TestSerdeConverter<byte[], Object, TestPlatformContext> {

        private final Class<byte[]> inputClass = byte[].class;
        private final Class<Object> outputClass = Object.class;
        private final Class<TestPlatformContext> contextClass = TestPlatformContext.class;

        @Override
        public Object convert(byte[] input, TestPlatformContext context) {
            if (input == null) {
                return null;
            }
            String text = new String(input, StandardCharsets.UTF_8);
            String tag = context.getChannel() + ":";
            return text.startsWith(tag) ? text.substring(tag.length()) : text;
        }
    }
}

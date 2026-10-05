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
public class PlatformContextSerializerProvider
        implements TestSerdeProviderFactory<Object, byte[], TestPlatformContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<Object> inputClass = Object.class;
    private final Class<byte[]> outputClass = byte[].class;
    private final Class<TestPlatformContext> contextClass = TestPlatformContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "platform-context";

    @Override
    public TestSerdeConverter<Object, byte[], TestPlatformContext> create(StandardSerdeProperties config,
                                                                          Class<Object> inputClass,
                                                                          Class<byte[]> outputClass,
                                                                          Class<TestPlatformContext> contextClass,
                                                                          @Nullable ComponentRole role) {
        return new PlatformContextSerializer();
    }

    @Getter
    public static class PlatformContextSerializer implements TestSerdeConverter<Object, byte[], TestPlatformContext> {

        private final Class<Object> inputClass = Object.class;
        private final Class<byte[]> outputClass = byte[].class;
        private final Class<TestPlatformContext> contextClass = TestPlatformContext.class;

        @Override
        public byte[] convert(Object input, TestPlatformContext context) {
            return input == null ? null : (context.getChannel() + ":" + input).getBytes(StandardCharsets.UTF_8);
        }
    }
}

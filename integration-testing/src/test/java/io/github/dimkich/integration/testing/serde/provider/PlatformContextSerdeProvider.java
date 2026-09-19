package io.github.dimkich.integration.testing.serde.provider;

import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;
import io.github.dimkich.integration.testing.serde.TestSerdeProvider;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import io.github.dimkich.integration.testing.serde.platform.TestPlatformContext;

import java.nio.charset.StandardCharsets;

public class PlatformContextSerdeProvider implements TestSerdeProvider<SerdeProperties> {
    @Override
    public String getName() {
        return "platform-context";
    }

    @Override
    public Object createSerializer(SerdeProperties config) {
        return new PlatformContextSerializer();
    }

    @Override
    public Object createDeserializer(SerdeProperties config) {
        return new PlatformContextDeserializer();
    }

    public static class PlatformContextSerializer implements TestSerdeSerializer<Object, TestPlatformContext> {
        @Override
        public Class<TestPlatformContext> getContextClass() {
            return TestPlatformContext.class;
        }

        @Override
        public byte[] serialize(Object data, TestPlatformContext context) {
            return data == null ? null : (context.getChannel() + ":" + data).getBytes(StandardCharsets.UTF_8);
        }
    }

    public static class PlatformContextDeserializer implements TestSerdeDeserializer<Object, TestPlatformContext> {
        @Override
        public Class<TestPlatformContext> getContextClass() {
            return TestPlatformContext.class;
        }

        @Override
        public Object deserialize(byte[] data, TestPlatformContext context) {
            if (data == null) {
                return null;
            }
            String text = new String(data, StandardCharsets.UTF_8);
            String tag = context.getChannel() + ":";
            return text.startsWith(tag) ? text.substring(tag.length()) : text;
        }
    }
}

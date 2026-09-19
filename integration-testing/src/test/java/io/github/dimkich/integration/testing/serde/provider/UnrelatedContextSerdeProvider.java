package io.github.dimkich.integration.testing.serde.provider;

import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;
import io.github.dimkich.integration.testing.serde.TestSerdeProvider;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import io.github.dimkich.integration.testing.serde.platform.UnrelatedSerdeContext;

import java.nio.charset.StandardCharsets;

public class UnrelatedContextSerdeProvider implements TestSerdeProvider<SerdeProperties> {
    @Override
    public String getName() {
        return "unrelated-context";
    }

    @Override
    public Object createSerializer(SerdeProperties config) {
        return new UnrelatedContextSerializer();
    }

    @Override
    public Object createDeserializer(SerdeProperties config) {
        return new UnrelatedContextDeserializer();
    }

    public static class UnrelatedContextSerializer implements TestSerdeSerializer<Object, UnrelatedSerdeContext> {
        @Override
        public Class<UnrelatedSerdeContext> getContextClass() {
            return UnrelatedSerdeContext.class;
        }

        @Override
        public byte[] serialize(Object data, UnrelatedSerdeContext context) {
            return data == null ? null : ("U:" + data).getBytes(StandardCharsets.UTF_8);
        }
    }

    public static class UnrelatedContextDeserializer implements TestSerdeDeserializer<Object, UnrelatedSerdeContext> {
        @Override
        public Class<UnrelatedSerdeContext> getContextClass() {
            return UnrelatedSerdeContext.class;
        }

        @Override
        public Object deserialize(byte[] data, UnrelatedSerdeContext context) {
            if (data == null) {
                return null;
            }
            String text = new String(data, StandardCharsets.UTF_8);
            return text.startsWith("U:") ? text.substring(2) : text;
        }
    }
}

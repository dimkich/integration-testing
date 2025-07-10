package io.github.dimkich.integration.testing.xml.config.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.github.dimkich.integration.testing.xml.polymorphic.PolymorphicStdSerializer;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.security.SecureRandom;

@JsonSerialize(using = SecureRandomMixIn.Serializer.class)
@JsonDeserialize(using = SecureRandomMixIn.Deserializer.class)
public class SecureRandomMixIn {

    static class Serializer extends PolymorphicStdSerializer<SecureRandom> {
        public Serializer() {
            super(new StdSerializer<>(SecureRandom.class) {
                @Override
                public void serialize(SecureRandom value, JsonGenerator gen, SerializerProvider provider) {
                }
            });
        }
    }

    static class Deserializer extends StdScalarDeserializer<SecureRandom> {
        private static SecureRandom secureRandom;

        public Deserializer() {
            super(HttpMethod.class);
        }

        @Override
        public SecureRandom deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (secureRandom == null) {
                secureRandom = new SecureRandom();
            }
            return secureRandom;
        }
    }
}

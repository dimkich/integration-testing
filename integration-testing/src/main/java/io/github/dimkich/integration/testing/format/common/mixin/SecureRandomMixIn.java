package io.github.dimkich.integration.testing.format.common.mixin;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;
import java.security.SecureRandom;

@JsonSerialize(using = SecureRandomMixIn.Serializer.class)
@JsonDeserialize(using = SecureRandomMixIn.Deserializer.class)
public class SecureRandomMixIn {

    static class Serializer extends StdScalarSerializer<SecureRandom> {
        public Serializer() {
            super(SecureRandom.class);
        }

        @Override
        public void serialize(SecureRandom value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString("");
        }
    }

    static class Deserializer extends StdScalarDeserializer<SecureRandom> {
        private static SecureRandom secureRandom;

        public Deserializer() {
            super(SecureRandom.class);
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

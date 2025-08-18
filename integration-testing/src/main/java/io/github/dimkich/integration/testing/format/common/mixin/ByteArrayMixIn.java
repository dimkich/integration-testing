package io.github.dimkich.integration.testing.format.common.mixin;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.github.dimkich.integration.testing.format.xml.polymorphic.PolymorphicStdSerializer;
import org.springframework.http.HttpMethod;

import java.io.IOException;

@JsonSerialize(using = ByteArrayMixIn.Serializer.class)
@JsonDeserialize(using = ByteArrayMixIn.Deserializer.class)
public class ByteArrayMixIn {
    static class Serializer extends PolymorphicStdSerializer<byte[]> {
        public Serializer() {
            super(new StdSerializer<>(byte[].class) {
                @Override
                public void serialize(byte[] value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                    gen.writeBinary(provider.getConfig().getBase64Variant(), value, 0, value.length);
                }
            });
        }
    }

    static class Deserializer extends StdScalarDeserializer<byte[]> {
        public Deserializer() {
            super(HttpMethod.class);
        }

        @Override
        public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return p.getBinaryValue();
        }
    }
}

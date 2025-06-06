package io.github.dimkich.integration.testing.xml;

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

@JsonSerialize(using = HttpMethodMixIn.Serializer.class)
@JsonDeserialize(using = HttpMethodMixIn.Deserializer.class)
public class HttpMethodMixIn {
    public static class Serializer extends PolymorphicStdSerializer<HttpMethod> {
        public Serializer() {
            super(new StdSerializer<>(HttpMethod.class) {
                @Override
                public void serialize(HttpMethod value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                    gen.writeString(value.name());
                }
            });
        }
    }

    public static class Deserializer extends StdScalarDeserializer<HttpMethod> {
        public Deserializer() {
            super(HttpMethod.class);
        }

        @Override
        public HttpMethod deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return HttpMethod.valueOf(p.getText());
        }
    }
}

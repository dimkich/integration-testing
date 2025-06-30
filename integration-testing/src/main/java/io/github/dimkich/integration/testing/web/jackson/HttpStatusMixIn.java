package io.github.dimkich.integration.testing.web.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.io.IOException;

@JsonDeserialize(using = HttpStatusMixIn.Deserializer.class)
public class HttpStatusMixIn {
    public static class Deserializer extends StdScalarDeserializer<HttpStatusCode> {
        public Deserializer() {
            super(HttpStatusCode.class);
        }

        @Override
        public HttpStatusCode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return HttpStatus.valueOf(p.getText());
        }
    }
}

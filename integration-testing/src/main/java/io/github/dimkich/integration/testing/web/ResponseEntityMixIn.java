package io.github.dimkich.integration.testing.web;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.charset.Charset;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"statusCode", "statusCodeValue", "headers", "body"})
public class ResponseEntityMixIn {
    @JsonCreator
    public ResponseEntityMixIn(
            @JsonProperty("body") Object body,
            @JsonDeserialize(using = MultiValueMapDeserializer.class) @JsonProperty("headers") MultiValueMap<String, String> headers,
            @JsonProperty("statusCode") HttpStatusCode status) {
    }

    @JsonDeserialize(using = HttpStatusMixIn.Deserializer.class)
    public static class HttpStatusMixIn {
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

    public static class MultiValueMapDeserializer extends JsonDeserializer<MultiValueMap<String, String>> {
        @Override
        public MultiValueMap<String, String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            while (p.nextToken() != JsonToken.END_OBJECT) {
                if (p.currentToken() == JsonToken.FIELD_NAME) {
                    p.nextToken();
                    map.add(p.currentName(), p.getValueAsString());
                }
            }
            return map.isEmpty() ? null : map;
        }
    }

    public static class RestClientResponseExceptionMixin {
        @JsonCreator
        public RestClientResponseExceptionMixin(
                @JsonProperty(value = "message", required = true) String message,
                @JsonSerialize(contentAs = HttpStatus.class) @JsonProperty(value = "statusCode", required = true) HttpStatusCode statusCode,
                @JsonProperty(value = "statusText", required = true) String statusText,
                @JsonProperty("headers") HttpHeaders headers, @JsonProperty("responseBody") byte[] responseBody,
                @JsonProperty("responseCharset") Charset responseCharset) {
        }
    }
}

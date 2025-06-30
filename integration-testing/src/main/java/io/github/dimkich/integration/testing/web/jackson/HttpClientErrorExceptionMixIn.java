package io.github.dimkich.integration.testing.web.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.util.Iterator;

@Getter(onMethod_ = @JsonIgnore)
@Setter(onMethod_ = @JsonIgnore)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"statusCode", "rawStatusCode", "responseHeaders", "message"})
@JsonDeserialize(using = HttpClientErrorExceptionMixIn.Deserializer.class)
public class HttpClientErrorExceptionMixIn {
    private Object statusText;
    private Object responseBody;
    private Object charset;

    public static class Deserializer extends StdDeserializer<HttpClientErrorException> {
        public Deserializer() {
            super(HttpClientErrorException.class);
        }

        @Override
        public HttpClientErrorException deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
            HttpHeaders headers = new HttpHeaders();
            JsonNode headersNode = node.get("responseHeaders");
            if (headersNode != null) {
                for (Iterator<String> iterator = headersNode.fieldNames(); iterator.hasNext(); ) {
                    String key = iterator.next();
                    headers.add(key, headersNode.get(key).asText());
                }
            }
            return HttpClientErrorException.create(
                    node.get("message") == null ? null : node.get("message").asText(),
                    HttpStatus.valueOf(node.get("statusCode").asText()),
                    "",
                    headers,
                    null,
                    null
            );
        }
    }
}

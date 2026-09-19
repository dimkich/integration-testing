package io.github.dimkich.integration.testing.web.jackson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.util.Iterator;

@SuppressWarnings("unused")
@Getter(onMethod_ = @JsonProperty)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"statusCode", "rawStatusCode", "responseHeaders", "message"})
@JsonDeserialize(using = HttpClientErrorExceptionMixIn.Deserializer.class)
public class HttpClientErrorExceptionMixIn {
    private HttpStatusCode statusCode;
    private int rawStatusCode;
    private HttpHeaders responseHeaders;
    private String message;

    /**
     * Jackson deserializer that restores {@link HttpClientErrorException} instances
     * from their serialized form.
     */
    public static class Deserializer extends StdDeserializer<HttpClientErrorException> {
        /**
         * Creates the deserializer.
         */
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
                    JsonNode value = headersNode.get(key);
                    if (value.isArray()) {
                        ArrayNode arrayNode = (ArrayNode) value;
                        for (int i = 0; i < arrayNode.size(); i++) {
                            headers.add(key, arrayNode.get(i).asText());
                        }
                    } else {
                        headers.add(key, value.asText());
                    }
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

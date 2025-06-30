package io.github.dimkich.integration.testing.web.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;

public class MultiValueMapDeserializer extends JsonDeserializer<MultiValueMap<String, String>> {
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

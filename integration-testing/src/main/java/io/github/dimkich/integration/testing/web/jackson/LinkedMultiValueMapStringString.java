package io.github.dimkich.integration.testing.web.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NoArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@JsonDeserialize(using = LinkedMultiValueMapStringString.MultiValueMapDeserializer.class)
public class LinkedMultiValueMapStringString extends LinkedMultiValueMap<String, String> {
    public LinkedMultiValueMapStringString(Map<String, List<String>> otherMap) {
        super(otherMap);
    }

    static class MultiValueMapDeserializer extends JsonDeserializer<LinkedMultiValueMapStringString> {
        @Override
        public LinkedMultiValueMapStringString deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            LinkedMultiValueMapStringString map = new LinkedMultiValueMapStringString();
            do {
                if (p.currentToken() == JsonToken.FIELD_NAME) {
                    String fieldName = p.currentName();
                    p.nextToken();
                    if (p.currentToken() == JsonToken.START_ARRAY) {
                        while (p.nextToken() != JsonToken.END_ARRAY) {
                            map.add(fieldName, p.getValueAsString());
                        }
                    } else {
                        map.add(fieldName, p.getValueAsString());
                    }
                }
            } while (p.nextToken() != JsonToken.END_OBJECT);
            return map.isEmpty() ? null : map;
        }
    }
}

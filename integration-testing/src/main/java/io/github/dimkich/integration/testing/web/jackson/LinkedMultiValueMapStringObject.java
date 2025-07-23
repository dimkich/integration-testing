package io.github.dimkich.integration.testing.web.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.util.LinkedMultiValueMap;

import java.io.IOException;

@JsonDeserialize(using = LinkedMultiValueMapStringObject.LinkedMultiValueMapStringObjectDeserializer.class)
public class LinkedMultiValueMapStringObject extends LinkedMultiValueMap<String, Object> {
    public static class LinkedMultiValueMapStringObjectDeserializer extends JsonDeserializer<LinkedMultiValueMapStringObject> {
        private final JavaType valueType = TypeFactory.defaultInstance().constructType(Object.class);

        @Override
        public LinkedMultiValueMapStringObject deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonDeserializer<Object> deserializer = ctxt.findRootValueDeserializer(valueType);
            LinkedMultiValueMapStringObject map = new LinkedMultiValueMapStringObject();
            if (p.currentToken() == JsonToken.FIELD_NAME) {
                p.nextToken();
                map.add(p.currentName(), deserializer.deserialize(p, ctxt));
            }
            while (p.nextToken() != JsonToken.END_OBJECT) {
                if (p.currentToken() == JsonToken.FIELD_NAME) {
                    p.nextToken();
                    map.add(p.currentName(), deserializer.deserialize(p, ctxt));
                }
            }
            return map.isEmpty() ? null : map;
        }
    }
}

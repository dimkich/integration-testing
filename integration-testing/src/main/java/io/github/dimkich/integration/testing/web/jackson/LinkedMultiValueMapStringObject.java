package io.github.dimkich.integration.testing.web.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.NoArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@JsonDeserialize(using = LinkedMultiValueMapStringObject.LinkedMultiValueMapStringObjectDeserializer.class)
public class LinkedMultiValueMapStringObject extends LinkedMultiValueMap<String, Object> {
    public LinkedMultiValueMapStringObject(Map<String, List<Object>> otherMap) {
        super(otherMap);
    }

    public static class LinkedMultiValueMapStringObjectDeserializer extends JsonDeserializer<LinkedMultiValueMapStringObject> {
        private final JavaType valueType = TypeFactory.defaultInstance().constructType(Object.class);

        @Override
        public LinkedMultiValueMapStringObject deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonDeserializer<Object> deserializer = ctxt.findRootValueDeserializer(valueType);
            LinkedMultiValueMapStringObject map = new LinkedMultiValueMapStringObject();
            do {
                if (p.currentToken() == JsonToken.FIELD_NAME) {
                    String fieldName = p.currentName();
                    p.nextToken();
                    if (p.currentToken() == JsonToken.START_ARRAY) {
                        while (p.nextToken() != JsonToken.END_ARRAY) {
                            map.add(fieldName, deserializer.deserialize(p, ctxt));
                        }
                    } else {
                        map.add(fieldName, deserializer.deserialize(p, ctxt));
                    }
                }
            } while ((p.nextToken() != JsonToken.END_OBJECT));
            return map.isEmpty() ? null : map;
        }
    }
}

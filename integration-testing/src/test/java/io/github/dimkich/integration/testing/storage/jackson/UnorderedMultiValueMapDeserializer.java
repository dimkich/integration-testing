package io.github.dimkich.integration.testing.storage.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.github.dimkich.integration.testing.storage.dto.UnorderedMultiValueMap;

import java.io.IOException;

/**
 * Reads an {@link UnorderedMultiValueMap} from named elements, including repeated names.
 */
public class UnorderedMultiValueMapDeserializer extends JsonDeserializer<UnorderedMultiValueMap> {
    private final JavaType valueType = TypeFactory.defaultInstance().constructType(Object.class);

    @Override
    public UnorderedMultiValueMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonDeserializer<Object> valueDeserializer = ctxt.findRootValueDeserializer(valueType);
        UnorderedMultiValueMap map = new UnorderedMultiValueMap();
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return map;
        }
        do {
            if (p.currentToken() == JsonToken.FIELD_NAME) {
                String name = p.currentName();
                p.nextToken();
                map.add(name, valueDeserializer.deserialize(p, ctxt));
            }
        } while (p.nextToken() != JsonToken.END_OBJECT);
        return map;
    }
}

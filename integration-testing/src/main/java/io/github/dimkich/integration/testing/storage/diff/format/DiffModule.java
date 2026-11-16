package io.github.dimkich.integration.testing.storage.diff.format;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.dimkich.integration.testing.storage.diff.model.DiffKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffObjectKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffResult;
import io.github.dimkich.integration.testing.storage.diff.model.DiffTextKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import io.github.dimkich.integration.testing.storage.diff.model.StorageDiff;

import java.io.IOException;
import java.util.Map;

/**
 * Codec of the storage difference model.
 * <p>
 * The codec depends on the format, so the module is assembled by a factory of that format. XML
 * writes the difference model with dynamic tag names and attributes, which XML supports and JSON
 * does not: a complete {@link DiffResult} keeps the storage tags, a single-storage
 * {@link StorageDiff} is one element with the storage name in the {@code storage} attribute. JSON
 * writes the model by the regular Jackson bean serialization and needs a codec only for a key: a
 * text key stays a plain string, and an object key is written as an object with its type,
 * attributes and body.
 */
public final class DiffModule extends SimpleModule {
    private DiffModule() {
        super("storage-diff");
    }

    /**
     * XML codec of the difference model.
     *
     * @return module of the XML mapper
     */
    public static DiffModule xml() {
        DiffModule module = new DiffModule();
        module.addSerializer(DiffResult.class, new XmlDiffWriter());
        module.addDeserializer(DiffResult.class, new XmlDiffReader());
        module.addSerializer(StorageDiff.class, new XmlStorageDiffWriter());
        module.addDeserializer(StorageDiff.class, new XmlStorageDiffReader());
        return module;
    }

    /**
     * JSON codec of keys; the rest of the model is written by regular bean serialization.
     *
     * @return module of the JSON mapper
     */
    public static DiffModule json() {
        DiffModule module = new DiffModule();
        module.addSerializer(DiffTextKey.class, new TextKeySerializer());
        module.addSerializer(DiffObjectKey.class, new ObjectKeySerializer());
        module.addDeserializer(DiffKey.class, new DiffKeyDeserializer());
        return module;
    }

    /**
     * Serialization view of an object key. A separate record keeps the declared {@link DiffValue}
     * type of the body, so Jackson writes its {@code type} property.
     */
    record ObjectKeyJson(String type, Map<String, String> attributes, DiffValue value) {
    }

    /**
     * Writes a text key as a plain string.
     */
    public static class TextKeySerializer extends JsonSerializer<DiffTextKey> {

        @Override
        public void serialize(DiffTextKey value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(value.text());
        }
    }

    /**
     * Writes an object key as a structure.
     */
    public static class ObjectKeySerializer extends JsonSerializer<DiffObjectKey> {
        @Override
        public void serialize(DiffObjectKey value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            serializers.defaultSerializeValue(
                    new ObjectKeyJson(value.type(), value.attributes(), value.value()), gen);
        }
    }

    /**
     * Reads a key: a string becomes a text key, an object becomes an object key.
     */
    public static class DiffKeyDeserializer extends JsonDeserializer<DiffKey> {

        @Override
        public DiffKey deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() == JsonToken.VALUE_STRING) {
                return new DiffTextKey(p.getText());
            }
            if (p.currentToken() == JsonToken.START_OBJECT) {
                return ctxt.readValue(p, DiffObjectKey.class);
            }
            throw JsonMappingException.from(p, "Difference key must be a string or an object");
        }
    }
}

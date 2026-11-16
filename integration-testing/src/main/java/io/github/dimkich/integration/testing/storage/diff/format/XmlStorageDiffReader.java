package io.github.dimkich.integration.testing.storage.diff.format;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.github.dimkich.integration.testing.format.xml.AttributeAwareParser;
import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import io.github.dimkich.integration.testing.storage.diff.model.StorageDiff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a {@link StorageDiff} written by {@link XmlStorageDiffWriter}.
 * <p>
 * The storage name is the {@code storage} attribute of the element; the element content is the
 * record entries, read by {@link XmlDiffEntryReader}. An obsolete {@code type} attribute is
 * ignored, so an element can keep the type marker of the previous format.
 */
class XmlStorageDiffReader extends JsonDeserializer<StorageDiff> {
    @Override
    public StorageDiff deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (!(p instanceof AttributeAwareParser attributeAware)) {
            throw new IllegalStateException("StorageDiff is read only from XML, but the parser is "
                    + p.getClass().getName());
        }
        XmlDiffEntryReader entryReader = new XmlDiffEntryReader(p);
        List<DiffEntry> entries = new ArrayList<>();
        String storage = null;
        JsonToken token = p.currentToken();
        if (token == JsonToken.START_OBJECT) {
            token = p.nextToken();
        }
        while (token == JsonToken.FIELD_NAME) {
            String name = p.currentName();
            if (attributeAware.isCurrentTokenAttribute()) {
                JsonToken valueToken = p.nextToken();
                if (valueToken != JsonToken.VALUE_STRING) {
                    throw JsonMappingException.from(p, "Attribute '" + name + "' must be a string");
                }
                if ("storage".equals(name)) {
                    storage = p.getText();
                }
                token = p.nextToken();
            } else {
                entries.add(entryReader.readEntry(name));
                token = p.nextToken();
            }
        }
        if (storage == null) {
            throw JsonMappingException.from(p, "Storage difference must have a 'storage' attribute; "
                    + "nested storage tags are not supported");
        }
        return new StorageDiff(storage, entries);
    }
}

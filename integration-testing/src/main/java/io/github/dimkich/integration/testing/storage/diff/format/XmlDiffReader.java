package io.github.dimkich.integration.testing.storage.diff.format;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.github.dimkich.integration.testing.format.xml.AttributeAwareParser;
import io.github.dimkich.integration.testing.storage.diff.model.DiffResult;

import java.io.IOException;

/**
 * Reads a {@link DiffResult} written by {@link XmlDiffWriter}.
 * <p>
 * The reader does not resolve value types: the type is an opaque attribute that round-trips
 * verbatim, so expected test data never needs the domain classes of the values.
 * <p>
 * The read model is normalized: some shapes are ambiguous in XML (for example, a keyless element
 * whose body consists of a field named {@code key}). A model read from XML serializes back to the
 * same text, but it is not guaranteed to be equal to the model that produced the XML. The
 * canonical form of a difference is its text.
 * <p>
 * The difference elements of one storage are read by {@link XmlDiffEntryReader}. Attributes are
 * distinguished from child elements through {@link AttributeAwareParser}.
 */
class XmlDiffReader extends JsonDeserializer<DiffResult> {
    @Override
    public DiffResult deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (!(p instanceof AttributeAwareParser)) {
            throw new IllegalStateException("DiffResult is read only from XML, but the parser is "
                    + p.getClass().getName());
        }
        DiffResult diff = new DiffResult();
        JsonToken token = p.currentToken();
        if (token == JsonToken.START_OBJECT) {
            token = p.nextToken();
        }
        while (token == JsonToken.FIELD_NAME) {
            String storage = p.currentName();
            JsonToken valueToken = p.nextToken();
            if (valueToken == JsonToken.START_OBJECT) {
                diff.put(storage, new XmlDiffEntryReader(p).readEntries());
            } else if (valueToken != JsonToken.VALUE_STRING || !p.getText().isEmpty()) {
                throw JsonMappingException.from(p, "Storage '" + storage + "' must be an element");
            }
            token = p.nextToken();
        }
        return diff;
    }
}

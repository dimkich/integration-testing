package io.github.dimkich.integration.testing.storage.diff.format;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import io.github.dimkich.integration.testing.storage.diff.model.DiffResult;
import io.github.dimkich.integration.testing.storage.diff.model.XmlNames;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Writes a {@link DiffResult} as an XML subtree: one element per storage, each difference element
 * written by {@link XmlDiffEntryWriter}.
 * <p>
 * The serializer itself is stateless: name answers are memoized in {@link XmlNames} and are safe
 * to share between threads.
 */
class XmlDiffWriter extends JsonSerializer<DiffResult> {
    @Override
    public void serialize(DiffResult value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        ToXmlGenerator xml = xmlGenerator(gen);
        xml.writeStartObject();
        writeStorages(xml, value, new XmlDiffEntryWriter(xml));
        xml.writeEndObject();
    }

    @Override
    public void serializeWithType(DiffResult value, JsonGenerator gen, SerializerProvider provider,
                                  TypeSerializer typeSer) throws IOException {
        ToXmlGenerator xml = xmlGenerator(gen);
        WritableTypeId typeId = typeSer.writeTypePrefix(xml, typeSer.typeId(value, JsonToken.START_OBJECT));
        writeStorages(xml, value, new XmlDiffEntryWriter(xml));
        typeSer.writeTypeSuffix(xml, typeId);
    }

    private ToXmlGenerator xmlGenerator(JsonGenerator gen) {
        if (gen instanceof ToXmlGenerator xml) {
            return xml;
        }
        throw new IllegalStateException("DiffResult is written only to XML, but the generator is "
                + gen.getClass().getName());
    }

    private void writeStorages(ToXmlGenerator gen, DiffResult value,
                               XmlDiffEntryWriter entryWriter) throws IOException {
        for (Map.Entry<String, List<DiffEntry>> storage : value.getStorages().entrySet()) {
            gen.writeFieldName(storage.getKey());
            gen.writeStartObject();
            for (DiffEntry entry : storage.getValue()) {
                entryWriter.write(entry, null);
            }
            gen.writeEndObject();
        }
    }
}

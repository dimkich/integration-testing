package io.github.dimkich.integration.testing.storage.diff.format;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import io.github.dimkich.integration.testing.storage.diff.model.StorageDiff;

import java.io.IOException;

/**
 * Writes one storage difference as an XML element: the {@code storage} attribute and the record
 * entries as the element content.
 * <p>
 * The entries are written by {@link XmlDiffEntryWriter}, so the element keeps the common storage
 * difference format without a nested storage tag.
 */
class XmlStorageDiffWriter extends JsonSerializer<StorageDiff> {
    @Override
    public void serialize(StorageDiff value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (!(gen instanceof ToXmlGenerator xml)) {
            throw new IllegalStateException("StorageDiff is written only to XML, but the generator is "
                    + gen.getClass().getName());
        }
        if (value.storage() == null) {
            throw new IllegalStateException("Storage name must not be null");
        }
        xml.writeStartObject();
        XmlDiffEntryWriter.writeAttribute(xml, "storage", value.storage());
        XmlDiffEntryWriter entryWriter = new XmlDiffEntryWriter(xml);
        for (DiffEntry entry : value.entries()) {
            entryWriter.write(entry, null);
        }
        xml.writeEndObject();
    }
}

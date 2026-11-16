package io.github.dimkich.integration.testing.storage.diff.format;

import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import io.github.dimkich.integration.testing.storage.diff.model.DiffObjectKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffOperation;
import io.github.dimkich.integration.testing.storage.diff.model.DiffTextKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffWriter;
import io.github.dimkich.integration.testing.storage.diff.model.XmlNames;
import lombok.SneakyThrows;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Writes one element of a storage difference as XML.
 * <p>
 * The tag of an element is its identifier when the identifier is usable as a tag and the operation
 * is inherited from the nearest operation tag. Otherwise, the tag is the operation: a key that is
 * not a tag, an operation that differs from the enclosing one, or a structured key. A storage has
 * no enclosing operation, so every record carries its operation tag.
 * <p>
 * A body of an {@code insert} or a {@code delete}, a key body and the value of a snapshot record
 * are snapshots: their elements are plain data rather than changes, so they are written by name.
 * An operation tag inside a snapshot is only a name wrapper for a field whose name cannot be used
 * as an XML tag.
 * <p>
 * The {@code key} and {@code type} attributes identify the value. The body is text, a nested
 * element set, nothing when a record or a field is deleted, or the removed value for a deleted
 * collection element or repeated field value.
 * <p>
 * A text key is written as the {@code key} attribute. An object key is written as a {@code <key>}
 * element and its value as a {@code <value>} element; such an element never carries the
 * {@code key} attribute, so the reader can tell the two forms apart. A key is a snapshot: its body
 * is written in the {@code insert} context, so the operation tag of a key field is only a name
 * wrapper.
 * <p>
 * The body and the key are written by themselves: a scalar becomes text, a {@code null} the
 * {@code xsi:nil} marker, a structured value its children, a text key an attribute or a tag and
 * an object key a nested element.
 * <p>
 * The state of the body is kept here because the serializer that creates the element is shared
 * between threads, so it cannot hold a body. One writer serves a whole result: nested elements
 * save and restore the state of their parent, so the writer is not allocated per element.
 */
final class XmlDiffEntryWriter implements DiffWriter {
    private final ToXmlGenerator gen;
    private DiffEntry entry;
    private String itemName;
    private DiffOperation operation;
    private DiffOperation parentOperation;
    private DiffOperation snapshotOperation;
    private String childItemName;
    private boolean hasBodyAttributes;

    XmlDiffEntryWriter(ToXmlGenerator gen) {
        this.gen = gen;
    }

    /**
     * Writes one element of a difference.
     *
     * @param itemName tag of the enclosing collection, used for elements without an identifier
     *                 and an operation tag
     */
    void write(DiffEntry entry, String itemName) throws IOException {
        DiffEntry previousEntry = this.entry;
        String previousItemName = this.itemName;
        DiffOperation previousOperation = this.operation;
        DiffOperation previousParentOperation = this.parentOperation;
        DiffOperation previousSnapshotOperation = this.snapshotOperation;
        String previousChildItemName = this.childItemName;
        boolean previousHasBodyAttributes = this.hasBodyAttributes;
        this.entry = entry;
        this.itemName = itemName;
        this.parentOperation = previousOperation;
        this.operation = entry.operation();
        try {
            if (entry.key() == null) {
                writeKeyless();
            } else {
                entry.key().write(this);
            }
        } finally {
            this.entry = previousEntry;
            this.itemName = previousItemName;
            this.operation = previousOperation;
            this.parentOperation = previousParentOperation;
            this.snapshotOperation = previousSnapshotOperation;
            this.childItemName = previousChildItemName;
            this.hasBodyAttributes = previousHasBodyAttributes;
        }
    }

    /**
     * Writes an element without a key: an item of a collection or a member of a sorted set.
     * <p>
     * An element whose operation differs from the enclosing one is written under the operation
     * tag, so an added or removed member keeps its meaning; {@code itemName} is used when the
     * operation is inherited or the element is a part of a snapshot.
     */
    private void writeKeyless() throws IOException {
        boolean inherited = snapshotOperation != null || operation == parentOperation;
        String tag = inherited ? itemNameOrItem() : operation.getTag();
        gen.writeFieldName(tag);
        gen.writeStartObject();
        boolean hasAttributes = writeAttributes(gen, entry.type(), entry.attributes());
        writeBody(entry.value(), itemName, hasAttributes, bodySnapshot());
        gen.writeEndObject();
    }

    @Override
    public void writeKey(DiffTextKey key) throws IOException {
        String text = key.text();
        DiffOperation snapshot = snapshotOperation;
        boolean keyAsTag = XmlNames.isTag(text)
                && (snapshot != null || operation == parentOperation);
        String tag = keyAsTag ? text : (snapshot != null ? snapshot : operation).getTag();
        gen.writeFieldName(tag);
        gen.writeStartObject();
        writeAttribute(gen, "key", keyAsTag ? null : text);
        boolean hasAttributes = writeAttributes(gen, entry.type(), entry.attributes());
        writeBody(entry.value(), keyAsTag ? text : itemName, hasAttributes, bodySnapshot());
        gen.writeEndObject();
    }

    @Override
    public void writeKey(DiffObjectKey key) throws IOException {
        DiffOperation snapshot = snapshotOperation;
        String tag = (snapshot != null ? snapshot : operation).getTag();
        gen.writeFieldName(tag);
        gen.writeStartObject();
        boolean hasAttributes = writeAttributes(gen, entry.type(), entry.attributes());
        writeObjectKey(key);
        if (entry.value() != null) {
            writeValueElement(entry.value(), hasAttributes);
        }
        gen.writeEndObject();
    }

    @Override
    public void writeText(String text) throws IOException {
        // An empty string is written as a text node, so the element keeps the full pair
        // <tag></tag>. The reader recovers the empty text only when the element has no type
        // or metadata attributes; otherwise the empty body stays self-closing.
        if (text != null && (!text.isEmpty() || !hasBodyAttributes)) {
            writeTextNode(gen, text);
        }
    }

    @Override
    public void writeNull() {
        writeNil(gen);
    }

    @Override
    public void writeFields(List<DiffEntry> fields) throws IOException {
        for (DiffEntry field : fields) {
            requireEntry(field);
            write(field, childItemName);
        }
    }

    @Override
    public void writeItems(List<DiffEntry> items) throws IOException {
        for (DiffEntry item : items) {
            requireEntry(item);
            write(item, childItemName);
        }
    }

    /**
     * @throws IllegalArgumentException when a structured body contains a {@code null} element
     */
    private static void requireEntry(DiffEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("A difference body must not contain null entries");
        }
    }

    /**
     * Writes the body into the current element and keeps the state the body needs.
     *
     * @param snapshot operation of the snapshot context for the body, or {@code null} when the
     *                 body is a difference
     */
    private void writeBody(DiffValue body, String childItemName,
                           boolean hasBodyAttributes, DiffOperation snapshot) throws IOException {
        if (body == null) {
            return;
        }
        DiffOperation previousSnapshot = this.snapshotOperation;
        this.childItemName = childItemName;
        this.hasBodyAttributes = hasBodyAttributes;
        this.snapshotOperation = snapshot;
        try {
            body.write(this);
        } finally {
            this.snapshotOperation = previousSnapshot;
        }
    }

    /**
     * Operation of the snapshot context for the body of the current element. An element already
     * inside a snapshot keeps its context; an {@code insert} or a {@code delete} outside a
     * snapshot opens its own snapshot. Any other operation means a difference body.
     */
    private DiffOperation bodySnapshot() {
        if (snapshotOperation != null) {
            return snapshotOperation;
        }
        return operation == DiffOperation.INSERT || operation == DiffOperation.DELETE
                ? operation
                : null;
    }

    /**
     * Writes the key of an element as a snapshot: type and attributes of the key, then its body.
     * The key body is written in the {@code insert} context, so its fields are written by name; an
     * operation tag is used only as a name wrapper.
     */
    private void writeObjectKey(DiffObjectKey key) throws IOException {
        gen.writeFieldName("key");
        gen.writeStartObject();
        boolean hasKeyAttributes = writeAttributes(gen, key.type(), key.attributes());
        writeBody(key.value(), null, hasKeyAttributes, DiffOperation.INSERT);
        gen.writeEndObject();
    }

    /**
     * Writes the value of an element with an object key. The type and metadata of the value
     * stay on the element itself, so the wrapper carries only the body.
     */
    private void writeValueElement(DiffValue value, boolean hasValueAttributes) throws IOException {
        gen.writeFieldName("value");
        gen.writeStartObject();
        writeBody(value, itemName, hasValueAttributes, bodySnapshot());
        gen.writeEndObject();
    }

    private String itemNameOrItem() {
        return itemName == null ? "item" : itemName;
    }

    /**
     * Writes the type and the metadata attributes of an element and reports whether any of them
     * was written. The model keeps the attributes in output order, so the writer only writes them.
     */
    private static boolean writeAttributes(ToXmlGenerator gen, String type,
                                           DiffAttributes attributes) throws IOException {
        boolean hasAttributes = writeAttribute(gen, "type", type);
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            hasAttributes |= writeAttribute(gen, attribute.getKey(), attribute.getValue());
        }
        gen.setNextIsAttribute(false);
        return hasAttributes;
    }

    /**
     * Writes one attribute and reports whether it was written.
     *
     * @return {@code true} when the value is not {@code null} and the attribute is written
     */
    static boolean writeAttribute(ToXmlGenerator gen, String name, String value)
            throws IOException {
        if (value == null) {
            return false;
        }
        gen.setNextIsAttribute(true);
        gen.writeStringField(name, value);
        gen.setNextIsAttribute(false);
        return true;
    }

    private static void writeTextNode(ToXmlGenerator gen, String text) throws IOException {
        gen.setNextIsUnwrapped(true);
        gen.writeFieldName("");
        gen.writeString(text);
        gen.setNextIsUnwrapped(false);
    }

    /**
     * Writes {@code xsi:nil="true"} on the current element. The generator writes a whole leaf
     * element for a {@code null} value, which does not fit an element that already carries
     * attributes, so the attribute is written through the underlying writer; the writer declares
     * the {@code xsi} namespace itself.
     */
    @SneakyThrows
    private static void writeNil(ToXmlGenerator gen) {
        gen.getStaxWriter().writeAttribute("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                "nil", "true");
    }
}

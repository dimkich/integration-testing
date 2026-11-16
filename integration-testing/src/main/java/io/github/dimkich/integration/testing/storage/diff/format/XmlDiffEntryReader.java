package io.github.dimkich.integration.testing.storage.diff.format;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.github.dimkich.integration.testing.format.xml.AttributeAwareParser;
import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import io.github.dimkich.integration.testing.storage.diff.model.DiffNullValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffOperation;
import io.github.dimkich.integration.testing.storage.diff.model.DiffScalarValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffTextKey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the difference elements of one element scope: a storage or a structured body.
 * <p>
 * The reader walks the tokens of an element and keeps the resulting state in
 * {@link XmlDiffEntryBuilder}: attributes arrive before the body, and the body shape is known only
 * when the closing tag is reached.
 */
final class XmlDiffEntryReader {
    private final JsonParser p;
    private final AttributeAwareParser attributeAware;

    XmlDiffEntryReader(JsonParser p) {
        this.p = p;
        this.attributeAware = p instanceof AttributeAwareParser parser ? parser : null;
    }

    /**
     * Reads all elements of the current object until its closing tag. An untagged element gets
     * the default {@code update} operation.
     *
     * @return difference elements in document order
     */
    List<DiffEntry> readEntries() throws IOException {
        List<DiffEntry> entries = new ArrayList<>(16);
        while (p.nextToken() == JsonToken.FIELD_NAME) {
            entries.add(readEntry(p.currentName()));
        }
        return entries;
    }

    /**
     * Reads one element.
     *
     * @param tag tag of the element, used as the operation or as the text key
     */
    DiffEntry readEntry(String tag) throws IOException {
        DiffOperation operation = DiffOperation.fromTag(tag);
        XmlDiffEntryBuilder entry = new XmlDiffEntryBuilder(operation,
                operation == null ? new DiffTextKey(tag) : null, operation != null);
        JsonToken token = p.nextToken();
        if (token == JsonToken.VALUE_STRING) {
            return entry.value(new DiffScalarValue(p.getText()));
        }
        if (token == JsonToken.VALUE_NULL) {
            return entry.value(DiffNullValue.INSTANCE);
        }
        if (token != JsonToken.START_OBJECT) {
            throw JsonMappingException.from(p, "Unexpected token " + token + " in <" + tag + ">");
        }
        readBody(entry, tag);
        return entry.build();
    }

    /**
     * Reads the content of an element: its attributes, text, null marker and child elements.
     */
    private void readBody(XmlDiffEntryBuilder entry, String tag) throws IOException {
        while (true) {
            JsonToken token = p.nextToken();
            if (token == null || token == JsonToken.END_OBJECT) {
                return;
            }
            if (token == JsonToken.VALUE_NULL) {
                entry.nil();
                continue;
            }
            if (token == JsonToken.VALUE_STRING) {
                entry.text(p.getText());
                continue;
            }
            if (token != JsonToken.FIELD_NAME) {
                throw JsonMappingException.from(p, "Unexpected token " + token + " in <" + tag + ">");
            }
            String name = p.currentName();
            if (isAttribute()) {
                if (readAttribute(entry, name)) {
                    entry.nil();
                }
            } else if (name.isEmpty()) {
                readText(entry, tag);
            } else {
                entry.child(readEntry(name));
            }
        }
    }

    /**
     * Reads the text node of an element, which the parser reports as an empty field name.
     */
    private void readText(XmlDiffEntryBuilder entry, String tag) throws IOException {
        JsonToken valueToken = p.nextToken();
        if (valueToken == JsonToken.VALUE_STRING) {
            entry.text(p.getText());
        } else if (valueToken == JsonToken.VALUE_NULL) {
            entry.nil();
        } else {
            throw JsonMappingException.from(p, "Unexpected text token " + valueToken
                    + " in <" + tag + ">");
        }
    }

    /**
     * Reads one attribute of the current element.
     * <p>
     * The XML parser reports {@code xsi:nil} under its local name {@code nil} and does not
     * collapse an element with several attributes to a null value, so the null marker is handled
     * by the builder. Namespace declarations are not part of the model.
     *
     * @return {@code true} when the attribute marks the element as {@code null}
     */
    private boolean readAttribute(XmlDiffEntryBuilder entry, String name) throws IOException {
        JsonToken token = p.nextToken();
        if (token != JsonToken.VALUE_STRING) {
            throw JsonMappingException.from(p, "Attribute '" + name + "' must be a string");
        }
        return entry.attribute(name, p.getText());
    }

    private boolean isAttribute() {
        return attributeAware != null && attributeAware.isCurrentTokenAttribute();
    }
}

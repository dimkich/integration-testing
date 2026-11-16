package io.github.dimkich.integration.testing.storage.diff.format;

import io.github.dimkich.integration.testing.storage.diff.model.DiffCollectionValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import io.github.dimkich.integration.testing.storage.diff.model.DiffKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffNullValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffObjectKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffObjectValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffOperation;
import io.github.dimkich.integration.testing.storage.diff.model.DiffScalarValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffTextKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable state of one difference element while it is read.
 * <p>
 * Attributes arrive before the body, and the body shape is known only when the closing tag is
 * reached. The builder also recognizes an object key: the first child {@code <key>} and, when the
 * value exists, the second child {@code <value>} are the key and the value of the element rather
 * than ordinary body fields.
 */
final class XmlDiffEntryBuilder {
    private final DiffOperation operation;
    private final boolean operationTag;
    private DiffKey key;
    private boolean keyFromAttribute;
    private String type;
    private Map<String, String> attributes;
    private List<DiffEntry> children;
    private String text;
    private boolean hasText;
    private boolean nil;
    private DiffValue value;

    /**
     * @param operation    operation taken from the tag, or {@code null} for a plain field
     * @param key          text key taken from the tag, or {@code null} for an operation element
     * @param operationTag {@code true} when the element was read from an operation tag
     */
    XmlDiffEntryBuilder(DiffOperation operation, DiffKey key, boolean operationTag) {
        this.operation = operation;
        this.key = key;
        this.operationTag = operationTag;
    }

    /**
     * Sets a ready body, used by an element whose whole content is a scalar.
     *
     * @param value body of the element
     * @return built element
     */
    DiffEntry value(DiffValue value) {
        this.value = value;
        return build();
    }

    void text(String text) {
        this.text = text;
        hasText = true;
    }

    void nil() {
        nil = true;
    }

    void child(DiffEntry child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }

    /**
     * Applies one attribute of the element.
     *
     * @return {@code true} when the attribute marks the element as {@code null}
     */
    boolean attribute(String name, String value) {
        if (name.equals("xmlns") || name.startsWith("xmlns:")) {
            return false;
        }
        if ("xsi:nil".equals(name) || "nil".equals(name)) {
            return Boolean.parseBoolean(value);
        }
        switch (name) {
            case "key" -> {
                key = new DiffTextKey(value);
                keyFromAttribute = true;
            }
            case "type" -> type = value;
            default -> {
                if (attributes == null) {
                    attributes = new LinkedHashMap<>();
                }
                attributes.put(name, value);
            }
        }
        return false;
    }

    /**
     * Builds the element and decides its body shape.
     *
     * @return difference element
     */
    DiffEntry build() {
        if (value == null) {
            boolean objectKey = promoteObjectKey();
            if (!objectKey) {
                value = bodyValue();
            }
        }
        return new DiffEntry(operation, key, type, attributes, value);
    }

    /**
     * Builds the body of an element that is not an object key.
     *
     * @return body of the element, or {@code null} for an empty delete
     */
    private DiffValue bodyValue() {
        if (nil) {
            return DiffNullValue.INSTANCE;
        }
        if (hasText) {
            return new DiffScalarValue(text);
        }
        if (children != null && !children.isEmpty()) {
            return toContainerValue();
        }
        if (operation == DiffOperation.DELETE) {
            return null;
        }
        // An empty body without a type and metadata is an empty text value, not an empty
        // object: the writer keeps the full pair for such values. A type or metadata
        // attribute means the element owns an empty structure or an attribute-only entry.
        return type == null && (attributes == null || attributes.isEmpty())
                ? new DiffScalarValue("")
                : new DiffObjectValue(List.of());
    }

    /**
     * Takes an object key from the first children of the element.
     * <p>
     * The method recognizes the shape produced by {@link XmlDiffEntryWriter}: the {@code <key>}
     * snapshot and, when the value exists, the {@code <value>} body. Any other set of children
     * stays an ordinary body, so a text-key element with a field named {@code key} keeps its usual
     * shape.
     *
     * @return {@code true} when the element is an object key
     */
    private boolean promoteObjectKey() {
        if (nil || hasText || !operationTag || keyFromAttribute
                || children == null || children.isEmpty()) {
            return false;
        }
        DiffEntry keyEntry = children.get(0);
        if (isWrapper(keyEntry, "key")) {
            if (children.size() == 1) {
                applyObjectKey(keyEntry, null);
                return true;
            }
            if (children.size() == 2 && isWrapper(children.get(1), "value")) {
                applyObjectKey(keyEntry, children.get(1));
                return true;
            }
        }
        return false;
    }

    /**
     * Moves the wrappers of an object key into the element: the key becomes a structure, the
     * type and metadata of the value move to the element itself.
     */
    private void applyObjectKey(DiffEntry keyEntry, DiffEntry valueEntry) {
        key = new DiffObjectKey(keyEntry.type(), keyEntry.attributes(), keyEntry.value());
        if (valueEntry != null) {
            value = valueEntry.value();
            if (type == null) {
                type = valueEntry.type();
            }
            if (!valueEntry.attributes().isEmpty()) {
                if (attributes == null) {
                    attributes = new LinkedHashMap<>(valueEntry.attributes().size());
                }
                attributes.putAll(valueEntry.attributes());
            }
        }
        children = null;
    }

    private boolean isWrapper(DiffEntry entry, String name) {
        return entry.operation() == DiffOperation.UPDATE
                && entry.key() instanceof DiffTextKey textKey
                && name.equals(textKey.text());
    }

    private DiffValue toContainerValue() {
        for (DiffEntry child : children) {
            if (child.key() != null) {
                return new DiffObjectValue(children);
            }
        }
        return new DiffCollectionValue(children);
    }
}

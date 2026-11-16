package io.github.dimkich.integration.testing.storage.diff.model;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

/**
 * Object key of a difference element: the key is written as a structure rather than a line of
 * text.
 * <p>
 * The key is a snapshot: its body has no differences, so the elements of its body are not change
 * markers. The XML writer writes them by name in the key context and uses an operation tag only as
 * a name wrapper for a field whose name is not a tag. The type and attributes describe the key,
 * the value is its body, or {@code null} for an attributes-only key.
 *
 * @param type       semantic type name of the key, may be {@code null}
 * @param attributes metadata attributes of the key, never {@code null}
 * @param value      body of the key, or {@code null} for an attributes-only key
 */
public record DiffObjectKey(String type, DiffAttributes attributes, DiffValue value) implements DiffKey {
    private static final Comparator<String> TEXTS =
            Comparator.nullsFirst(Comparator.naturalOrder());
    private static final Comparator<DiffValue> VALUES =
            Comparator.nullsFirst(Comparator.naturalOrder());

    public DiffObjectKey {
        attributes = attributes == null ? DiffAttributes.EMPTY : attributes;
    }

    /**
     * Creates a key with metadata attributes.
     *
     * @param type       semantic type name of the key, may be {@code null}
     * @param attributes metadata attributes of the key
     * @param value      body of the key, or {@code null} for an attributes-only key
     */
    public DiffObjectKey(String type, Map<String, String> attributes, DiffValue value) {
        this(type, DiffAttributes.of(attributes), value);
    }

    @Override
    public int rank() {
        return 0;
    }

    @Override
    public int compareContent(DiffKey other) {
        DiffObjectKey that = (DiffObjectKey) other;
        int order = TEXTS.compare(type, that.type);
        if (order != 0) {
            return order;
        }
        order = attributes.compareTo(that.attributes);
        return order != 0 ? order : VALUES.compare(value, that.value);
    }

    @Override
    public String description() {
        return "object key";
    }

    @Override
    public void write(DiffWriter writer) throws IOException {
        writer.writeKey(this);
    }
}

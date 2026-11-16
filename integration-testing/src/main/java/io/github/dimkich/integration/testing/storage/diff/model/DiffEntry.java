package io.github.dimkich.integration.testing.storage.diff.model;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * One element of a storage difference: a record, a field of a record or a collection element.
 * <p>
 * The element has an identifier, an operation and a value:
 * <ul>
 *     <li>{@link #operation} - operation applied to the element; every element has one,
 *         {@link DiffOperation#UPDATE} when it is not set explicitly;</li>
 *     <li>{@link #key} - identifier of the element within its parent: record key, field name,
 *         map key or keyed collection member; a {@link DiffTextKey} for a line of text, an
 *         {@link DiffObjectKey} for a structure, {@code null} for an element of a collection;</li>
 *     <li>{@link #type} - value type written as the {@code type} attribute;</li>
 *     <li>{@link #attributes} - metadata attributes by name, written after {@code key} and
 *         {@code type};</li>
 *     <li>{@link #value} - body of the element, or {@code null} when a record or a field is
 *         deleted.</li>
 * </ul>
 * <p>
 * Elements are comparable: the natural order is deterministic and is used only for output order.
 * Elements compare by key, operation, type, attributes and value, in that order. The order is
 * consistent with {@code equals}.
 *
 * @param operation  operation applied to the element, {@code null} means
 *                   {@link DiffOperation#UPDATE}
 * @param key        identifier of the element within its parent
 * @param type       value type, or {@code null} when the value has no explicit type
 * @param attributes metadata attributes by name, never {@code null}
 * @param value      body of the element; {@code null} only when a record or a field is deleted: a
 *                   removed element of a collection or a repeated value of a multi-value map
 *                   keeps its body
 */
public record DiffEntry(DiffOperation operation, DiffKey key, String type, DiffAttributes attributes,
                        DiffValue value) implements Comparable<DiffEntry> {
    private static final Comparator<DiffKey> KEYS =
            Comparator.nullsFirst(Comparator.naturalOrder());
    private static final Comparator<DiffOperation> OPERATIONS = Comparator.naturalOrder();
    private static final Comparator<String> TEXTS =
            Comparator.nullsFirst(Comparator.naturalOrder());
    private static final Comparator<DiffValue> VALUES =
            Comparator.nullsFirst(Comparator.naturalOrder());

    public DiffEntry {
        operation = operation == null ? DiffOperation.UPDATE : operation;
        attributes = attributes == null ? DiffAttributes.EMPTY : attributes;
    }

    /**
     * Creates an element with metadata attributes.
     *
     * @param operation  operation applied to the element, {@code null} means
     *                   {@link DiffOperation#UPDATE}
     * @param key        identifier of the element, may be {@code null} for a collection element
     * @param type       value type, or {@code null}
     * @param attributes metadata attributes
     * @param value      body of the element, may be {@code null} when a record or a field is deleted
     */
    public DiffEntry(DiffOperation operation, DiffKey key, String type, Map<String, String> attributes,
                     DiffValue value) {
        this(operation, key, type, DiffAttributes.of(attributes), value);
    }

    /**
     * Creates an element with a text key.
     *
     * @param operation  operation applied to the element, {@code null} means
     *                   {@link DiffOperation#UPDATE}
     * @param key        identifier of the element, may be {@code null} for a collection element
     * @param type       value type, or {@code null}
     * @param attributes metadata attributes
     * @param value      body of the element, may be {@code null} when a record or a field is deleted
     */
    public DiffEntry(DiffOperation operation, String key, String type, Map<String, String> attributes,
                     DiffValue value) {
        this(operation, key == null ? null : new DiffTextKey(key), type, attributes, value);
    }

    /**
     * Creates an element without metadata attributes.
     *
     * @param operation operation applied to the element, {@code null} means
     *                  {@link DiffOperation#UPDATE}
     * @param key       identifier of the element, may be {@code null} for a collection element
     * @param value     body of the element, may be {@code null} when a record or a field is deleted
     * @return difference element
     */
    public static DiffEntry of(DiffOperation operation, String key, DiffValue value) {
        return new DiffEntry(operation, key, null, Map.of(), value);
    }

    /**
     * Creates an element with an explicitly null body.
     *
     * @param operation operation applied to the element
     * @param key       identifier of the element within its parent
     * @return element with the {@code xsi:nil} body
     */
    public static DiffEntry nil(DiffOperation operation, DiffKey key) {
        return new DiffEntry(operation, key, null, DiffAttributes.EMPTY, DiffNullValue.INSTANCE);
    }

    @Override
    public int compareTo(DiffEntry other) {
        int order = KEYS.compare(key, other.key);
        if (order != 0) {
            return order;
        }
        order = OPERATIONS.compare(operation, other.operation);
        if (order != 0) {
            return order;
        }
        order = TEXTS.compare(type, other.type);
        if (order != 0) {
            return order;
        }
        order = attributes.compareTo(other.attributes);
        return order != 0 ? order : VALUES.compare(value, other.value);
    }

    /**
     * Compares two lists element by element, shorter list first. The lists are ordered by their
     * elements, so the method belongs to the element that the lists consist of.
     *
     * @param first  first list
     * @param second second list
     * @param <T>    element type
     * @return comparison result
     */
    static <T extends Comparable<? super T>> int compareLists(List<T> first, List<T> second) {
        int order = Integer.compare(first.size(), second.size());
        if (order != 0) {
            return order;
        }
        for (int i = 0; i < first.size(); i++) {
            order = first.get(i).compareTo(second.get(i));
            if (order != 0) {
                return order;
            }
        }
        return 0;
    }
}

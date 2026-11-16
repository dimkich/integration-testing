package io.github.dimkich.integration.testing.storage.diff.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Operation performed on a record, field or collection element in a storage difference.
 * <p>
 * The operation defines the XML tag of the element: {@code insert}, {@code update} or
 * {@code delete}. Every element has an operation: an element without an explicit one gets
 * {@link #UPDATE}. The writer omits the tag when the operation is inherited from the nearest
 * operation tag and the key is usable as an XML tag. The body of an {@code insert} or a
 * {@code delete} and a key body are snapshots: their elements are not change markers, and the
 * writer writes them by name.
 */
public enum DiffOperation {
    INSERT("insert"),
    UPDATE("update"),
    DELETE("delete");

    private final String tag;

    DiffOperation(String tag) {
        this.tag = tag;
    }

    /**
     * @return XML tag that represents this operation
     */
    @JsonValue
    public String getTag() {
        return tag;
    }

    /**
     * @param tag XML tag name
     * @return operation registered under the tag, or {@code null} when the tag is not an operation
     */
    @JsonCreator
    public static DiffOperation fromTag(String tag) {
        for (DiffOperation operation : values()) {
            if (operation.tag.equals(tag)) {
                return operation;
            }
        }
        return null;
    }
}

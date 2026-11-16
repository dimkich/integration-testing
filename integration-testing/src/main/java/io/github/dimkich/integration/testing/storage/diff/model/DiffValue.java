package io.github.dimkich.integration.testing.storage.diff.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.IOException;

/**
 * Body of a {@link DiffEntry}.
 * <p>
 * The body is one of four shapes: a scalar text, an explicit {@code null}, an object with
 * named fields or a collection with unnamed elements. The value type that is written as the
 * {@code type} attribute lives on the owning {@link DiffEntry}, not here.
 * <p>
 * Bodies are comparable: the natural order is deterministic and is used only for output order.
 * Values of different shapes are ordered by {@link #rank()}, values of the same shape by their
 * content. The order is consistent with {@code equals}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(DiffScalarValue.class),
        @JsonSubTypes.Type(DiffNullValue.class),
        @JsonSubTypes.Type(DiffObjectValue.class),
        @JsonSubTypes.Type(DiffCollectionValue.class)
})
public sealed interface DiffValue extends Comparable<DiffValue>
        permits DiffScalarValue, DiffNullValue, DiffObjectValue, DiffCollectionValue {

    /**
     * Writes the body into the given sink.
     * <p>
     * What the body consists of depends only on its own shape, so a consumer calls this method
     * instead of checking the type of the value.
     *
     * @param writer sink of the body
     */
    void write(DiffWriter writer) throws IOException;

    /**
     * @return position of the shape in the natural order: explicit null, scalar, object,
     * collection
     */
    int rank();

    /**
     * A structured body is empty when it has no fields or items. A scalar and an explicit
     * {@code null} always carry content, even an empty text, so they are never empty.
     *
     * @return {@code true} when a structured body has no fields or items
     */
    @JsonIgnore
    boolean isEmpty();

    /**
     * Compares the content with another body of the same shape.
     *
     * @param other body with the same {@link #rank()}
     * @return comparison result
     */
    int compareContent(DiffValue other);

    @Override
    default int compareTo(DiffValue other) {
        int order = Integer.compare(rank(), other.rank());
        return order != 0 ? order : compareContent(other);
    }
}

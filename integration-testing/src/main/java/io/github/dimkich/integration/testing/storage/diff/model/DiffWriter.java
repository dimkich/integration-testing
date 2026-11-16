package io.github.dimkich.integration.testing.storage.diff.model;

import java.io.IOException;
import java.util.List;

/**
 * Sink of a difference body or key.
 * <p>
 * A body and a key know their own shape, so they describe themselves through this interface and
 * the consumer never checks their type. The value operations write a body into the current
 * element, the key operations write the element of the key itself.
 */
public interface DiffWriter {

    /**
     * Writes a value that is a single text.
     *
     * @param text text of the value
     */
    void writeText(String text) throws IOException;

    /**
     * Writes an explicit {@code null} value.
     */
    void writeNull();

    /**
     * Writes named fields of an object or keyed elements of a map.
     *
     * @param fields fields in output order
     */
    void writeFields(List<DiffEntry> fields) throws IOException;

    /**
     * Writes unnamed elements of a collection.
     *
     * @param items elements in output order
     */
    void writeItems(List<DiffEntry> items) throws IOException;

    /**
     * Writes a key that is a single line of text.
     *
     * @param key text key
     */
    void writeKey(DiffTextKey key) throws IOException;

    /**
     * Writes a key that is a structure.
     *
     * @param key object key
     */
    void writeKey(DiffObjectKey key) throws IOException;
}

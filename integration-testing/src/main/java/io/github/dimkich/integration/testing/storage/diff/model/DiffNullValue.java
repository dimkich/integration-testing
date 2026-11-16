package io.github.dimkich.integration.testing.storage.diff.model;

/**
 * Explicit {@code null} value of a difference entry. Written as {@code xsi:nil="true"} in XML.
 */
public record DiffNullValue() implements DiffValue {

    /**
     * The single instance: the record has no components, so every instance is equal to it.
     */
    public static final DiffNullValue INSTANCE = new DiffNullValue();

    @Override
    public int rank() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int compareContent(DiffValue other) {
        return 0;
    }

    /**
     * Writes the {@code null} marker.
     */
    @Override
    public void write(DiffWriter writer) {
        writer.writeNull();
    }
}

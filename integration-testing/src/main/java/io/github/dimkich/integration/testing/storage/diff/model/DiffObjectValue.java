package io.github.dimkich.integration.testing.storage.diff.model;

import java.io.IOException;
import java.util.List;

/**
 * Structured value of a difference entry: named fields of an object or keyed elements of a map.
 *
 * @param fields fields in output order
 */
public record DiffObjectValue(List<DiffEntry> fields) implements DiffValue {

    public DiffObjectValue {
        fields = fields == null ? List.of() : fields;
    }

    @Override
    public int rank() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        return fields.isEmpty();
    }

    @Override
    public int compareContent(DiffValue other) {
        return DiffEntry.compareLists(fields, ((DiffObjectValue) other).fields);
    }

    /**
     * Writes the named fields of the value.
     */
    @Override
    public void write(DiffWriter writer) throws IOException {
        writer.writeFields(fields);
    }
}

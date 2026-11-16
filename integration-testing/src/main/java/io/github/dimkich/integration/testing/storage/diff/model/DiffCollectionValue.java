package io.github.dimkich.integration.testing.storage.diff.model;

import java.io.IOException;
import java.util.List;

/**
 * Collection value of a difference entry: unnamed elements of a list, set or sorted set.
 *
 * @param items elements in output order
 */
public record DiffCollectionValue(List<DiffEntry> items) implements DiffValue {

    public DiffCollectionValue {
        items = items == null ? List.of() : items;
    }

    @Override
    public int rank() {
        return 4;
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public int compareContent(DiffValue other) {
        return DiffEntry.compareLists(items, ((DiffCollectionValue) other).items);
    }

    /**
     * Writes the unnamed elements of the value.
     */
    @Override
    public void write(DiffWriter writer) throws IOException {
        writer.writeItems(items);
    }
}

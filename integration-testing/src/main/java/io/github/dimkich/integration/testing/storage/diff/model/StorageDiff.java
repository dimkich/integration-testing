package io.github.dimkich.integration.testing.storage.diff.model;

import java.util.List;

/**
 * Difference of one storage in the test file: the storage name and the record entries.
 * <p>
 * The storage name is written as the {@code storage} attribute of the element, and the entries
 * are the element content. A storage without changes is not written at all.
 *
 * @param storage storage name from the test configuration
 * @param entries record entries of the storage
 */
public record StorageDiff(String storage, List<DiffEntry> entries) {
}

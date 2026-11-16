package io.github.dimkich.integration.testing.storage.diff.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Root of a storage difference: storage names in alphabetical order, each with an ordered list of
 * record entries.
 * <p>
 * A test file writes one storage per element as a {@link StorageDiff}; this class keeps the
 * complete result of the diff engine and its own test fixture.
 * <p>
 * Storages without changes do not appear in the map; an empty difference is not written at all.
 */
public class DiffResult {
    @Getter(onMethod_ = @JsonAnyGetter)
    private final Map<String, List<DiffEntry>> storages = new TreeMap<>();

    /**
     * Adds records of one storage, replacing a previously added list.
     *
     * @param name    storage name from the test configuration
     * @param entries record entries of the storage
     */
    @JsonAnySetter
    public void put(String name, List<DiffEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            storages.remove(name);
            return;
        }
        storages.put(name, entries);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return storages.isEmpty();
    }
}

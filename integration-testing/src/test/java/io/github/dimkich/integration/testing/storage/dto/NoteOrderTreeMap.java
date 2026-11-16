package io.github.dimkich.integration.testing.storage.dto;

import io.github.dimkich.integration.testing.format.common.map.JsonMapAsEntries;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * Sorted map with object keys ordered by a field that is not part of the rendered key: the
 * difference engine keeps the comparator order of a sorted map.
 */
@JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ELEMENT, entriesWrapped = true)
public class NoteOrderTreeMap<K, V> extends TreeMap<K, V> {
    public NoteOrderTreeMap() {
        super(Comparator.comparing(key -> ((Item) key).getNote()));
    }
}

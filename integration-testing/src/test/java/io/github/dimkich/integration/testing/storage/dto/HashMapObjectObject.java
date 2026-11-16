package io.github.dimkich.integration.testing.storage.dto;

import io.github.dimkich.integration.testing.format.common.map.JsonMapAsEntries;

import java.util.HashMap;

/**
 * Hash based map with object keys: the difference engine has to sort its keys by the rendered
 * identifier.
 */
@JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ELEMENT, entriesWrapped = true)
public class HashMapObjectObject<K, V> extends HashMap<K, V> {
}

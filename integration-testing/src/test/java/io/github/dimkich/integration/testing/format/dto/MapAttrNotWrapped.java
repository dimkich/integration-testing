package io.github.dimkich.integration.testing.format.dto;

import io.github.dimkich.integration.testing.format.common.map.JsonMapAsEntries;

import java.util.LinkedHashMap;

@JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ATTRIBUTE)
public class MapAttrNotWrapped<K, V> extends LinkedHashMap<K, V> {
}

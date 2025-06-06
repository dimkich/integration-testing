package io.github.dimkich.integration.testing.storage.mapping;

import java.util.function.Function;

public class MapStringKeyObjectValue extends MapContainer<String, Object> {
    @Override
    public void addEntry(ChangeType change, Object key, Object value, Function<Object, String> toString) {
        map.put(toString.apply(key), value);
    }
}
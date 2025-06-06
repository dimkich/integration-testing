package io.github.dimkich.integration.testing.storage.mapping;

import java.util.function.Function;

public class EntriesStringKeyObjectValue extends EntriesContainer<EntryStringKeyObjectValue> {
    @Override
    public void addEntry(ChangeType change, Object key, Object value, Function<Object, String> toString) {
        entry.add(new EntryStringKeyObjectValue(toString.apply(key), change, value));
    }
}

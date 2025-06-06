package io.github.dimkich.integration.testing.storage.mapping;

import java.util.function.Function;

public class EntriesObjectKeyObjectValue extends EntriesContainer<EntryObjectKeyObjectValue>{
    @Override
    public void addEntry(ChangeType change, Object key, Object value, Function<Object, String> toString) {
        entry.add(new EntryObjectKeyObjectValue(change, key, value));
    }
}

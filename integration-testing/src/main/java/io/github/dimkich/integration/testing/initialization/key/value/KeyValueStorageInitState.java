package io.github.dimkich.integration.testing.initialization.key.value;

import io.github.dimkich.integration.testing.initialization.TestInitState;
import io.github.dimkich.integration.testing.storage.keyvalue.KeyValueDataStorage;
import lombok.Getter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class KeyValueStorageInitState implements TestInitState<KeyValueStorageInitState> {
    private final Map<KeyValueDataStorage, StorageState> storageStates = new HashMap<>();

    static KeyValueStorageInitState of(KeyValueDataStorage storage, KeyValueStorageInit init) {
        KeyValueStorageInitState state = new KeyValueStorageInitState();
        state.storageStates.put(storage, StorageState.of(init));
        return state;
    }

    @Override
    public KeyValueStorageInitState merge(KeyValueStorageInitState state) {
        for (Map.Entry<KeyValueDataStorage, StorageState> entry : state.storageStates.entrySet()) {
            storageStates.compute(entry.getKey(), (k, v) -> v == null ? entry.getValue() : v.merge(entry.getValue()));
        }
        return this;
    }

    @Override
    public KeyValueStorageInitState copy() {
        KeyValueStorageInitState copy = new KeyValueStorageInitState();
        storageStates.forEach((k, v) -> copy.storageStates.put(k, v.copy()));
        return copy;
    }

    @Getter
    static class StorageState {
        private Map<String, Object> map;
        private boolean clear;

        static StorageState of(KeyValueStorageInit init) {
            StorageState storageState = new StorageState();
            storageState.map = init.getMap();
            storageState.clear = init.getClear() != null && init.getClear();
            return storageState;
        }

        StorageState merge(StorageState state) {
            if (state.clear) {
                clear = true;
                if (map != null) {
                    map.clear();
                }
            }
            if (state.map != null) {
                map = map == null ? new LinkedHashMap<>() : map;
                map.putAll(state.map);
            }
            return this;
        }

        StorageState copy() {
            StorageState copy = new StorageState();
            if (map != null) {
                copy.map = new LinkedHashMap<>();
                copy.map.putAll(map);
            }
            copy.clear = clear;
            return copy;
        }
    }
}

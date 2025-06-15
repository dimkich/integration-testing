package io.github.dimkich.integration.testing.storage.keyvalue;

import io.github.dimkich.integration.testing.TestDataStorage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.Map;

@RequiredArgsConstructor
public class KeyValueDataStorageService implements TestDataStorage {
    private final KeyValueDataStorage storage;

    @Override
    public String getName() {
        return storage.getName();
    }

    @Override
    @SneakyThrows
    public Map<String, Object> getCurrentValue() {
        return storage.getKeysData();
    }

    @SneakyThrows
    public void setData(Map<String, Object> data) {
        storage.putKeysData(data);
    }

    @SneakyThrows
    public void clear() {
        storage.clearAll();
    }
}

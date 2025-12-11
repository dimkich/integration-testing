package io.github.dimkich.integration.testing.initialization.key.value;

import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.initialization.InitSetup;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.keyvalue.KeyValueDataStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class KeyValueStorageInitSetup implements InitSetup<KeyValueStorageInit, KeyValueStorageInitState> {
    private final TestDataStorages testDataStorages;

    @Override
    public Class<KeyValueStorageInit> getTestCaseInitClass() {
        return KeyValueStorageInit.class;
    }

    @Override
    public KeyValueStorageInitState defaultState() {
        return new KeyValueStorageInitState();
    }

    @Override
    public KeyValueStorageInitState convert(KeyValueStorageInit init) {
        KeyValueDataStorage storage = testDataStorages.getTestDataStorage(init.getName(), KeyValueDataStorage.class);
        return KeyValueStorageInitState.of(storage, init);
    }

    @Override
    public void apply(KeyValueStorageInitState oldState, KeyValueStorageInitState newState, Test test) throws Exception {
        for (Map.Entry<KeyValueDataStorage, KeyValueStorageInitState.StorageState> entry : newState.getStorageStates().entrySet()) {
            KeyValueDataStorage storage = entry.getKey();
            if (entry.getValue().isClear()) {
                log.debug("Storage {} is cleared", storage.getName());
                storage.clearAll();
                testDataStorages.addAffectedStorage(storage);
            }
            if (entry.getValue().getMap() != null && !entry.getValue().getMap().isEmpty()) {
                log.debug("Data loaded to storage {}", storage.getName());
                storage.putKeysData(entry.getValue().getMap());
                testDataStorages.addAffectedStorage(storage);
            }
        }
    }

    @Override
    public Integer getOrder() {
        return 3000;
    }
}

package io.github.dimkich.integration.testing.initialization.key.value;

import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.initialization.InitSetup;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.keyvalue.KeyValueDataStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Implementation of {@link InitSetup} for key-value storage initialization in integration tests.
 * This class handles the setup and application of key-value storage configurations during test execution.
 *
 * <p>Key-value storage initialization allows test configurations to clear existing data and/or load
 * initial key-value pairs into key-value storage systems (such as Redis, in-memory caches, etc.)
 * before test execution. This is useful for setting up test data in key-value stores and ensuring
 * a clean state for each test.
 *
 * <p>This setup implementation:
 * <ul>
 *   <li>Converts {@link KeyValueStorageInit} configurations into {@link KeyValueStorageInitState} objects</li>
 *   <li>Clears storage data when the {@code clear} flag is set</li>
 *   <li>Loads key-value pairs into storage from the configuration</li>
 *   <li>Tracks affected storages for test cleanup purposes</li>
 *   <li>Runs with an execution order of 3000, allowing earlier initializations (like date/time) to occur first</li>
 * </ul>
 *
 * <p>The initialization process follows these steps:
 * <ol>
 *   <li>If the {@code clear} flag is set, all existing data in the storage is removed</li>
 *   <li>If a map of key-value pairs is provided, the data is loaded into the storage</li>
 *   <li>Affected storages are registered with {@link TestDataStorages} for potential cleanup after test execution</li>
 * </ol>
 *
 * @author dimkich
 * @see InitSetup
 * @see KeyValueStorageInit
 * @see KeyValueStorageInitState
 * @see KeyValueDataStorage
 * @see TestDataStorages
 */
@Slf4j
@RequiredArgsConstructor
public class KeyValueStorageInitSetup implements InitSetup<KeyValueStorageInit, KeyValueStorageInitState> {
    /**
     * The test data storages registry used to retrieve storage instances and track affected storages.
     */
    private final TestDataStorages testDataStorages;

    /**
     * Returns the class of test initialization configuration that this setup handles.
     *
     * @return the {@link KeyValueStorageInit} class
     */
    @Override
    public Class<KeyValueStorageInit> getTestCaseInitClass() {
        return KeyValueStorageInit.class;
    }

    /**
     * Returns a default empty state for key-value storage initialization.
     * This is used when no previous state exists or as a base for merging states.
     *
     * @return a new empty {@link KeyValueStorageInitState} instance
     */
    @Override
    public KeyValueStorageInitState defaultState() {
        return new KeyValueStorageInitState();
    }

    /**
     * Converts a {@link KeyValueStorageInit} configuration into a {@link KeyValueStorageInitState} object.
     * This method retrieves the storage instance by name from the test data storages registry
     * and creates a state object containing the storage and its initialization configuration.
     *
     * @param init the key-value storage initialization configuration to convert
     * @return a state object containing the storage instance and its initialization state
     * @throws Exception if the storage cannot be retrieved or if the conversion fails
     */
    @Override
    public KeyValueStorageInitState convert(KeyValueStorageInit init) {
        KeyValueDataStorage storage = testDataStorages.getTestDataStorage(init.getName(), KeyValueDataStorage.class);
        return KeyValueStorageInitState.of(storage, init);
    }

    /**
     * Applies the key-value storage initialization state to a test by clearing storage data
     * and/or loading key-value pairs into the storage.
     *
     * <p>This method:
     * <ul>
     *   <li>Iterates through all storage states in the new state</li>
     *   <li>Clears storage data if the {@code clear} flag is set</li>
     *   <li>Loads key-value pairs into storage if a map is provided and not empty</li>
     *   <li>Registers affected storages with {@link TestDataStorages} for cleanup tracking</li>
     *   <li>Logs debug information for each operation</li>
     * </ul>
     *
     * <p>The {@code oldState} parameter is ignored as storage initialization is applied
     * independently based on the new state configuration.
     *
     * @param oldState the previous state (ignored in this implementation)
     * @param newState the new state containing storage configurations to apply
     * @param test     the test to apply the initialization to
     * @throws Exception if clearing storage fails or if loading data into storage fails
     */
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

    /**
     * Returns the execution order for this setup relative to other initialization setups.
     *
     * <p>Returns 3000, which places this setup in the middle of the execution order,
     * allowing earlier initializations (such as date/time with order 0) to occur first,
     * but before later initializations (such as bean initialization with order 10000).
     * Lower order values are executed before higher ones.
     *
     * @return the order value {@code 3000}
     */
    @Override
    public Integer getOrder() {
        return 3000;
    }
}

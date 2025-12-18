package io.github.dimkich.integration.testing.initialization.key.value;

import io.github.dimkich.integration.testing.initialization.TestInitState;
import io.github.dimkich.integration.testing.storage.keyvalue.KeyValueDataStorage;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the state of key-value storage initialization for integration tests.
 * This class maintains a mapping of {@link KeyValueDataStorage} instances to their
 * corresponding {@link StorageState}, which includes the data to be loaded and
 * whether the storage should be cleared before initialization.
 * <p>
 * This state object is used during test initialization to track and merge
 * initialization configurations from multiple sources, allowing for composition
 * of initialization states across different test configurations.
 * <p>
 * The state supports merging multiple initialization configurations, where
 * later configurations can override or extend earlier ones. When merging,
 * if a storage has a clear flag set, it will clear existing data before
 * applying new data.
 *
 * @author dimkich
 * @see TestInitState
 * @see KeyValueStorageInit
 * @see KeyValueDataStorage
 */
@Getter
@ToString
public class KeyValueStorageInitState implements TestInitState<KeyValueStorageInitState> {
    /**
     * Map of key-value storage instances to their initialization states.
     * Each entry associates a {@link KeyValueDataStorage} with its
     * {@link StorageState}, which contains the data to load and whether
     * to clear the storage before initialization.
     */
    private final Map<KeyValueDataStorage, StorageState> storageStates = new HashMap<>();

    /**
     * Creates a new {@code KeyValueStorageInitState} instance from a storage
     * and initialization configuration.
     *
     * @param storage the key-value storage instance to initialize
     * @param init    the initialization configuration containing data and clear flag
     * @return a new state instance containing the storage and its initialization state
     */
    static KeyValueStorageInitState of(KeyValueDataStorage storage, KeyValueStorageInit init) {
        KeyValueStorageInitState state = new KeyValueStorageInitState();
        state.storageStates.put(storage, StorageState.of(init));
        return state;
    }

    /**
     * Merges this state with another state instance.
     * <p>
     * For each storage in the provided state:
     * <ul>
     *   <li>If the storage doesn't exist in this state, it is added with its state</li>
     *   <li>If the storage already exists, its state is merged with the existing state</li>
     * </ul>
     * <p>
     * The merge operation allows combining initialization configurations from
     * multiple sources, with later configurations potentially overriding or
     * extending earlier ones.
     *
     * @param state the state to merge with this state
     * @return this state instance after merging (for method chaining)
     */
    @Override
    public KeyValueStorageInitState merge(KeyValueStorageInitState state) {
        for (Map.Entry<KeyValueDataStorage, StorageState> entry : state.storageStates.entrySet()) {
            storageStates.compute(entry.getKey(), (k, v) -> v == null ? entry.getValue() : v.merge(entry.getValue()));
        }
        return this;
    }

    /**
     * Creates a deep copy of this state instance.
     * <p>
     * The copy includes all storage states, and each storage state is also
     * deeply copied, ensuring that modifications to the copy do not affect
     * the original state.
     *
     * @return a new state instance that is a deep copy of this state
     */
    @Override
    public KeyValueStorageInitState copy() {
        KeyValueStorageInitState copy = new KeyValueStorageInitState();
        storageStates.forEach((k, v) -> copy.storageStates.put(k, v.copy()));
        return copy;
    }

    /**
     * Represents the initialization state for a single key-value storage.
     * This includes the data to be loaded (as a map of key-value pairs) and
     * a flag indicating whether the storage should be cleared before loading data.
     * <p>
     * The state supports merging operations where:
     * <ul>
     *   <li>If the merged state has {@code clear} set to {@code true}, the current
     *       state's clear flag is set and any existing map data is cleared</li>
     *   <li>If the merged state has map data, it is merged into the current map,
     *       with entries from the merged state potentially overriding existing entries</li>
     * </ul>
     */
    @Getter
    static class StorageState {
        /**
         * Map of key-value pairs to load into the storage.
         * Keys are strings and values can be any object type.
         * If {@code null}, no data will be loaded.
         */
        private Map<String, Object> map;

        /**
         * Whether to clear all existing data from the storage before loading new data.
         * If {@code true}, the storage will be cleared before the data from {@code map}
         * is loaded.
         */
        private boolean clear;

        /**
         * Creates a new {@code StorageState} instance from an initialization configuration.
         *
         * @param init the initialization configuration containing data and clear flag
         * @return a new storage state instance initialized with the configuration data
         */
        static StorageState of(KeyValueStorageInit init) {
            StorageState storageState = new StorageState();
            storageState.map = init.getMap();
            storageState.clear = init.getClear() != null && init.getClear();
            return storageState;
        }

        /**
         * Merges this storage state with another storage state.
         * <p>
         * The merge operation:
         * <ul>
         *   <li>If the provided state has {@code clear} set to {@code true}, sets this
         *       state's clear flag to {@code true} and clears any existing map data</li>
         *   <li>If the provided state has map data, merges it into this state's map.
         *       If this state's map is {@code null}, creates a new {@link LinkedHashMap}.
         *       Entries from the provided state will override existing entries with
         *       the same keys</li>
         * </ul>
         *
         * @param state the storage state to merge with this state
         * @return this storage state instance after merging (for method chaining)
         */
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

        /**
         * Creates a deep copy of this storage state instance.
         * <p>
         * The copy includes a deep copy of the map (if present) and copies the clear flag.
         * Modifications to the copy's map will not affect the original map.
         *
         * @return a new storage state instance that is a deep copy of this state
         */
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

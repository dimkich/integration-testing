package io.github.dimkich.integration.testing.storage.keyvalue;

import io.github.dimkich.integration.testing.TestDataStorage;

import java.util.Map;

/**
 * {@link TestDataStorage} implementation that works with key-value data.
 * <p>
 * Implementations are responsible for storing and clearing arbitrary
 * key-value pairs used in integration tests.
 */
public interface KeyValueDataStorage extends TestDataStorage {

    /**
     * Stores the provided key-value pairs in the underlying storage.
     *
     * @param map key-value pairs to store; keys must be unique within the map
     * @throws Exception if the data cannot be stored
     */
    void putKeysData(Map<String, Object> map) throws Exception;

    /**
     * Removes all data from the underlying storage.
     *
     * @throws Exception if the storage cannot be cleared
     */
    void clearAll() throws Exception;
}

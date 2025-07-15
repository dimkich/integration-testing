package io.github.dimkich.integration.testing.storage.keyvalue;

import io.github.dimkich.integration.testing.TestDataStorage;

import java.util.Map;

public interface KeyValueDataStorage extends TestDataStorage {

    void putKeysData(Map<String, Object> map) throws Exception;

    void clearAll() throws Exception;
}

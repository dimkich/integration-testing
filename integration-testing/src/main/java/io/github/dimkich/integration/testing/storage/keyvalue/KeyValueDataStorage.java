package io.github.dimkich.integration.testing.storage.keyvalue;

import java.util.Map;

public interface KeyValueDataStorage {
    String getName();

    Map<String, Object> getKeysData() throws Exception;

    void putKeysData(Map<String, Object> map) throws Exception;

    void clearAll() throws Exception;
}

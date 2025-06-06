package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.TestCaseMapper;
import io.github.dimkich.integration.testing.TestDataStorage;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TestDataStorages {
    private final Map<String, TestDataStorage> storageMap;
    private final ObjectsDifference objectsDifference;
    @Setter
    private TestCaseMapper testCaseMapper;
    private Map<String, Map<Object, Object>> currentValue = new LinkedHashMap<>();

    public TestDataStorages(Map<String, TestDataStorage> storageMap, ObjectsDifference objectsDifference) {
        this.storageMap = storageMap.entrySet().stream()
                .map(e -> Pair.of(e.getKey().startsWith("#") ? e.getKey().substring(1) : e.getKey(), e.getValue()))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (x, y) -> y, LinkedHashMap::new));
        this.objectsDifference = objectsDifference;
    }

    public <T extends TestDataStorage> T getTestDataStorage(String name, Class<T> cls) {
        TestDataStorage storage = storageMap.get(name);
        if (storage == null) {
            throw new RuntimeException(String.format("TestDataStorage '%s' not found", name));
        }
        return cls.cast(storage);
    }

    public void clear() {
        currentValue.clear();
        storageMap.values().forEach(TestDataStorage::clear);
    }

    public void addTestDataStorage(TestDataStorage testDataStorage) {
        storageMap.put(testDataStorage.getName(), testDataStorage);
    }

    public Object getMapDiff() {
        Map<String, Map<Object, Object>> currentValue = getCurrentValue();
        Object diff = objectsDifference.getDifference(this.currentValue, currentValue);
        this.currentValue = currentValue;
        return diff;
    }

    private Map<String, Map<Object, Object>> getCurrentValue() {
        return storageMap.values().stream()
                .filter(ds -> !ds.isEmpty())
                .collect(Collectors.toMap(TestDataStorage::getName, s -> testCaseMapper.deepClone(s.getCurrentValue()),
                        (v1, v2) -> v2, LinkedHashMap::new));
    }
}

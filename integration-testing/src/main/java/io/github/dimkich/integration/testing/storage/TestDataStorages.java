package io.github.dimkich.integration.testing.storage;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import io.github.dimkich.integration.testing.TestDataStorage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TestDataStorages {
    private final Map<String, TestDataStorage> storageMap;
    private final ObjectsDifference objectsDifference;

    private Map<String, Map<String, Object>> currentValue = new LinkedHashMap<>();

    @PostConstruct
    void init() {
        Map<String, TestDataStorage> map = storageMap.values().stream()
                .collect(Collectors.toMap(TestDataStorage::getName, Function.identity(), (x, y) -> y, LinkedHashMap::new));
        storageMap.clear();
        storageMap.putAll(map);
    }

    public <T extends TestDataStorage> T getTestDataStorage(String name, Class<T> cls) {
        TestDataStorage storage = storageMap.get(name);
        if (storage == null) {
            throw new RuntimeException(String.format("TestDataStorage '%s' not found", name));
        }
        return cls.cast(storage);
    }

    public Object getMapDiff() {
        Map<String, Map<String, Object>> currentValue = getCurrentValue();
        Object diff = objectsDifference.getDifference(this.currentValue, currentValue);
        this.currentValue = currentValue;
        return diff;
    }

    public void setNewCurrentValue(String name) throws Exception {
        TestDataStorage testDataStorage = getTestDataStorage(name, TestDataStorage.class);
        currentValue.put(name, testDataStorage.getCurrentValue());
    }

    private Map<String, Map<String, Object>> getCurrentValue() {
        return storageMap.values().stream()
                .collect(Collectors.toMap(TestDataStorage::getName, SneakyFunction.sneaky(TestDataStorage::getCurrentValue),
                        (v1, v2) -> v2, LinkedHashMap::new));
    }
}

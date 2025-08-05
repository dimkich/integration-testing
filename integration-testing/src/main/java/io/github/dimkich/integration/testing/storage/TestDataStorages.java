package io.github.dimkich.integration.testing.storage;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import io.github.dimkich.integration.testing.TestDataStorage;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import io.github.dimkich.integration.testing.storage.mapping.Container;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TestDataStorages {
    private final static Supplier<Map<String, Map<String, Object>>> ordered = LinkedHashMap::new;
    private final static Supplier<Map<String, Map<String, Object>>> sorted = TreeMap::new;

    private final Map<String, TestDataStorage> storageMap;
    private final ObjectsDifference objectsDifference;
    private final StorageProperties properties;
    private final TestExecutor testExecutor;

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
        if (diff instanceof Container container) {
            container.clearNullValueKeys();
        }
        this.currentValue = currentValue;
        return diff;
    }

    public void setNewCurrentValue() {
        this.currentValue = getCurrentValue();
    }

    private Map<String, Map<String, Object>> getCurrentValue() {
        testExecutor.setExecuting(true);
        try {
            return storageMap.values().stream()
                    .collect(Collectors.toMap(
                            TestDataStorage::getName,
                            SneakyFunction.sneaky(s -> s.getCurrentValue(properties.getExcludedFields(s.getName()))),
                            (v1, v2) -> v2,
                            properties.getSort(0) ? sorted : ordered
                    ));
        } finally {
            testExecutor.setExecuting(false);
        }
    }
}

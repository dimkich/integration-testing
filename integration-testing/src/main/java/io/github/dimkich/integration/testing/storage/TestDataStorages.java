package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.TestDataStorage;
import io.github.dimkich.integration.testing.storage.mapping.Container;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
@RequiredArgsConstructor
public class TestDataStorages {
    private final Map<String, TestDataStorage> storageMap;
    private final StorageProperties properties;

    @PostConstruct
    void init() {
        Map<String, TestDataStorage> map = new LinkedHashMap<>();
        for (TestDataStorage storage : storageMap.values()) {
            TestDataStorage previous = map.putIfAbsent(storage.getName(), storage);
            if (previous != null) {
                throw new IllegalStateException("Duplicate TestDataStorage name '" + storage.getName()
                        + "': " + previous.getClass().getName() + " and " + storage.getClass().getName());
            }
        }
        storageMap.clear();
        storageMap.putAll(map);
    }

    public <T extends TestDataStorage> T getTestDataStorage(String name, Class<T> cls) {
        TestDataStorage storage = storageMap.get(name);
        if (storage == null) {
            throw new IllegalStateException(String.format("TestDataStorage '%s' not found", name));
        }
        return cls.cast(storage);
    }

    public <T extends TestDataStorage> Stream<T> getTestDataStorages(Class<T> cls) {
        return storageMap.values().stream().filter(cls::isInstance).map(cls::cast);
    }

    public Object getMapDiff() throws Exception {
        Container rootContainer = Container.create(
                properties.getKeyType(0), properties.getValueType(0),
                properties.getSort(0), properties.getChangeType(0));

        for (TestDataStorage storage : storageMap.values()) {
            Object storageDiff = storage.getDiff();
            if (storageDiff != null) {
                rootContainer.addEntry(Container.ChangeType.changed, storage.getName(), storageDiff, String::valueOf);
            }
        }
        rootContainer.clearNullValueKeys();
        return rootContainer.isEmpty() ? null : rootContainer;
    }

    /**
     * Prepares all storages for the upcoming test. Snapshot storages refresh their baseline;
     * other storages keep the default no-op implementation.
     */
    public void prepareForTest() throws Exception {
        for (TestDataStorage storage : storageMap.values()) {
            storage.prepareForTest();
        }
    }
}

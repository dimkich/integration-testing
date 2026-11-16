package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.TestDataStorage;
import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import io.github.dimkich.integration.testing.storage.diff.model.StorageDiff;
import io.github.dimkich.integration.testing.util.FastPath;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
@RequiredArgsConstructor
public class TestDataStorages {
    private final Map<String, TestDataStorage> storageMap;

    @PostConstruct
    void init() {
        Map<String, TestDataStorage> map = new LinkedHashMap<>(FastPath.hashCapacity(storageMap.size()));
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

    public List<StorageDiff> getDiff() throws Exception {
        List<StorageDiff> diff = new ArrayList<>();
        for (TestDataStorage storage : storageMap.values()) {
            List<DiffEntry> entries = storage.getDiff();
            if (entries != null && !entries.isEmpty()) {
                diff.add(new StorageDiff(storage.getName(), entries));
            }
        }
        diff.sort(Comparator.comparing(StorageDiff::storage));
        return diff.isEmpty() ? null : diff;
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

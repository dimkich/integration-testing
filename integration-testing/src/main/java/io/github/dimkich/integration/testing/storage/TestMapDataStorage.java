package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.TestDataStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public class TestMapDataStorage implements TestDataStorage {
    private final String name;
    @Getter
    private final Map<Object, Object> map;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<Object, Object> getCurrentValue() {
        return new LinkedHashMap<>(map);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public void clear() {
        map.clear();
    }
}

package io.github.dimkich.integration.testing.redis.redisson;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.cache.processor.MutableEntry;
import java.util.Map;

@RequiredArgsConstructor
public class MapMutableEntry<K, V> implements MutableEntry<K, V> {
    @Getter
    private final K key;
    private final Map<K, V> map;

    @Override
    public boolean exists() {
        return map.containsKey(getKey());
    }

    @Override
    public void remove() {
        map.remove(getKey());
    }

    @Override
    public V getValue() {
        return map.get(getKey());
    }

    @Override
    public void setValue(V value) {
        map.put(getKey(), value);
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        return null;
    }
}

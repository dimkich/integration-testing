package io.github.dimkich.integration.testing.redis.redisson.convert;

import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public class MapBridge implements RBridge {
    private final Map<Object, Object> map;

    @Override
    public void set(Object value) {
        map.putAll((Map<?, ?>) value);
    }

    @Override
    public Object get() {
        return new LinkedHashMap<>(map);
    }

    @Override
    public void clear() {
        map.clear();
    }
}

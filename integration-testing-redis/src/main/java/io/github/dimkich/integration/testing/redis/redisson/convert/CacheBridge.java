package io.github.dimkich.integration.testing.redis.redisson.convert;

import lombok.RequiredArgsConstructor;

import javax.cache.Cache;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class CacheBridge implements RBridge {
    private final Cache<Object, Object> cache;

    @Override
    public void set(Object value) {
        cache.putAll((Map<?, ?>) value);
    }

    @Override
    public Object get() {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (Cache.Entry<Object, Object> entry : cache) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public void excludeFields(Set<String> fields) {
        cache.removeAll(fields);
    }
}

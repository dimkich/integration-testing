package io.github.dimkich.integration.testing.redis.spring;

import org.springframework.data.map.MapKeyValueAdapter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LinkedMapKeyValueAdapter extends MapKeyValueAdapter {
    private final Map<String, Map<Object, Object>> store = new ConcurrentHashMap<>();

    @Override
    protected Map<Object, Object> getKeySpaceMap(String keyspace) {
        return store.computeIfAbsent(keyspace, k -> Collections.synchronizedMap(new LinkedHashMap()));
    }
}

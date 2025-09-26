package io.github.dimkich.integration.testing.redis.redisson;

import io.github.dimkich.integration.testing.redis.redisson.convert.RBridge;
import io.github.dimkich.integration.testing.redis.redisson.convert.RBridgeFactory;
import io.github.dimkich.integration.testing.storage.keyvalue.KeyValueDataStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.redisson.api.RObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RedissonDataStorage implements KeyValueDataStorage {
    @Getter
    private final String name;
    private final Map<String, RBridge> redissonObjects = new LinkedHashMap<>();

    @Override
    public Map<String, Object> getCurrentValue(Map<String, Set<String>> excludedFields) {
        return redissonObjects.entrySet().stream()
                .map(e -> {
                    Set<String> fields = excludedFields.get(e.getKey());
                    if (fields != null) {
                        e.getValue().excludeFields(fields);
                    }
                    return Pair.of(e.getKey(), e.getValue().get());
                })
                .filter(p -> p.getValue() != null)
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    @Override
    public void putKeysData(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            RBridge bridge = redissonObjects.get(entry.getKey());
            bridge.set(entry.getValue());
        }
    }

    @Override
    public void clearAll() {
        for (RBridge object : redissonObjects.values()) {
            object.clear();
        }
    }

    void tryPut(Object object) {
        if (object instanceof RObject rObject) {
            redissonObjects.put(rObject.getName(), RBridgeFactory.create(rObject));
        }
    }
}

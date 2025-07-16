package io.github.dimkich.integration.testing.redis.redisson.convert;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.redisson.api.RMultimap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class MultimapBridge implements RBridge {
    private final RMultimap<Object, Object> multimap;

    @Override
    @SuppressWarnings("unchecked")
    public void set(Object value) {
        Map<Object, Object> map = (Map<Object, Object>) value;
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            multimap.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Object get() {
        return multimap.keySet().stream()
                .map(key -> Pair.of(key, multimap.get(key)))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (a, b) -> a, LinkedHashMap::new));
    }

    @Override
    public void clear() {
        multimap.clear();
    }

    @Override
    public void excludeFields(Set<String> fields) {
        multimap.keySet().removeAll(fields);
    }
}

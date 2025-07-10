package io.github.dimkich.integration.testing.storage.keyvalue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
public class KeyValueOperationsDataStorage implements KeyValueDataStorage {
    @Getter
    private final String name;
    private final KeyValueOperations keyValueOperations;

    @Override
    public Map<String, Object> getKeysData() {
        return keyValueOperations.getMappingContext().getPersistentEntities().stream()
                .map(pe -> (KeyValuePersistentEntity<?, ?>) pe)
                .filter(pe -> pe.getKeySpace() != null)
                .flatMap(pe -> {
                    Iterable<?> iterable = keyValueOperations.getKeyValueAdapter().getAllOf(pe.getKeySpace());
                    return StreamSupport.stream(iterable.spliterator(), false)
                            .map(o -> Map.entry(pe.getKeySpace() + "_"
                                    + pe.getIdentifierAccessor(o).getRequiredIdentifier(), o));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2,
                        LinkedHashMap::new));
    }

    @Override
    public void putKeysData(Map<String, Object> map) {
        map.forEach((k, v) -> {
            if (v instanceof Map<?, ?> m) {
                m.values().forEach(keyValueOperations::update);
            } else if (v instanceof Collection<?> c) {
                c.forEach(keyValueOperations::update);
            } else {
                throw new RuntimeException("Unsupported type: " + v.getClass());
            }
        });
    }

    @Override
    public void clearAll() {
        keyValueOperations.getMappingContext().getPersistentEntities().stream()
                .map(pe -> (KeyValuePersistentEntity<?, ?>) pe)
                .map(KeyValuePersistentEntity::getKeySpace)
                .filter(Objects::nonNull)
                .forEach(ks -> keyValueOperations.getKeyValueAdapter().deleteAllOf(ks));
    }
}

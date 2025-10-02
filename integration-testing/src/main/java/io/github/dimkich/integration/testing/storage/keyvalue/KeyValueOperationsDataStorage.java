package io.github.dimkich.integration.testing.storage.keyvalue;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import io.github.dimkich.integration.testing.util.TestUtils;
import io.github.sugarcubes.cloner.Cloner;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.map.MapKeyValueAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
public class KeyValueOperationsDataStorage implements KeyValueDataStorage {
    @Getter
    private final String name;
    private final KeyValueOperations keyValueOperations;
    private final Cloner cloner;

    @Override
    public Map<String, Object> getCurrentValue(Map<String, Set<String>> excludedFields) {
        boolean clone = keyValueOperations.getKeyValueAdapter() instanceof MapKeyValueAdapter;
        return keyValueOperations.getMappingContext().getPersistentEntities().stream()
                .map(pe -> (KeyValuePersistentEntity<?, ?>) pe)
                .filter(pe -> pe.getKeySpace() != null)
                .flatMap(pe -> {
                    Iterable<?> iterable = keyValueOperations.getKeyValueAdapter().getAllOf(pe.getKeySpace());
                    Set<String> excluded = excludedFields.get(pe.getKeySpace());
                    return StreamSupport.stream(iterable.spliterator(), false)
                            .map(o -> clone ? cloner.clone(o) : o)
                            .map(SneakyFunction.sneaky(o -> {
                                if (excluded != null) {
                                    for (String field : excluded) {
                                        setNull(pe, field, o);
                                    }
                                }
                                return Map.entry(pe.getKeySpace() + "_"
                                        + pe.getIdentifierAccessor(o).getRequiredIdentifier(), o);
                            }));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2,
                        LinkedHashMap::new));
    }

    @Override
    public void putKeysData(Map<String, Object> map) {
        boolean clone = keyValueOperations.getKeyValueAdapter() instanceof MapKeyValueAdapter;
        map.forEach((k, v) -> {
            if (v instanceof Map<?, ?> m) {
                m.values().stream().map(o -> clone ? cloner.clone(o) : o).forEach(keyValueOperations::update);
            } else if (v instanceof Collection<?> c) {
                c.stream().map(o -> clone ? cloner.clone(o) : o).forEach(keyValueOperations::update);
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

    private void setNull(KeyValuePersistentEntity<?, ?> pe, String fieldName, Object o) throws InvocationTargetException,
            IllegalAccessException {
        KeyValuePersistentProperty<?> property = pe.getPersistentProperty(fieldName);
        if (property == null) {
            throw new RuntimeException(String.format("Property '%s' not found in keyspace '%s'", fieldName,
                    pe.getKeySpace()));
        }
        Object value = TestUtils.getDefaultValue(property.getActualType());
        Method method = property.getSetter();
        if (method != null) {
            method.invoke(o, value);
            return;
        }
        Field field = property.getField();
        if (field != null) {
            field.set(o, value);
            return;
        }
        throw new RuntimeException(String.format("Property '%s' in keyspace '%s' is inaccessible", fieldName,
                pe.getKeySpace()));
    }
}

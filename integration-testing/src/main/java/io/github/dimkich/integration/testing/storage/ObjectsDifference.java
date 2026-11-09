package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.comparison.ObjectComparisonService;
import io.github.dimkich.integration.testing.date.time.PeriodDuration;
import io.github.dimkich.integration.testing.storage.mapping.Container;
import io.github.dimkich.integration.testing.util.CollectionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ObjectsDifference {
    private final StorageProperties properties;
    private final JacksonConverter jacksonConverter;
    private final ObjectComparisonService objectComparisonService;
    private final Function<Object, Object> nonStringKeysConverter = Function.identity();
    private final Function<Object, Boolean> simpleTypeDetector = o -> BeanUtils.isSimpleValueType(o.getClass())
            || o.getClass() == PeriodDuration.class || o.getClass() == byte[].class;

    public Object getDifference(String storageName, Object left, Object right) {
        return diff(storageName, left, right, 1);
    }

    private Object diff(String storageName, Object left, Object right, int level) {
        if (right == null) {
            return null;
        }
        if ((left == null || simpleTypeDetector.apply(left)) && simpleTypeDetector.apply(right)) {
            return right;
        }
        if ((left == null || left instanceof Map<?, ?>) && right instanceof Map<?, ?>) {
            return mapDiff(storageName, left == null ? Map.of() : (Map<?, ?>) left, (Map<?, ?>) right, level);
        }
        if (right instanceof Set<?> rightSet) {
            return setDiff(storageName, left instanceof Set<?> ? (Set<?>) left : Set.of(), rightSet, level);
        }
        if ((left == null || left instanceof Collection<?>) && right instanceof Collection<?>) {
            return right;
        }
        return left == null ? right : pojoDiff(storageName, left, right, level);
    }

    private Object mapDiff(String storageName, Map<?, ?> left, Map<?, ?> right, int level) {
        Container container = Container.create(properties.getKeyType(storageName, level),
                properties.getValueType(storageName, level), properties.getSort(storageName, level),
                properties.getChangeType(storageName, level));
        CollectionUtils.setsDifference(right.keySet(), left.keySet()).forEach(key ->
                container.addEntry(Container.ChangeType.added, key, diff(storageName, null, right.get(key), level + 1),
                        this::convertKey));
        CollectionUtils.setsIntersection(right.keySet(), left.keySet()).forEach(key -> {
            Object leftValue = left.get(key);
            Object rightValue = right.get(key);
            if (!objectComparisonService.isEquals(leftValue, rightValue)) {
                container.addEntry(Container.ChangeType.changed, key,
                        diff(storageName, leftValue, rightValue, level + 1), this::convertKey);
            }
        });
        CollectionUtils.setsDifference(left.keySet(), right.keySet()).forEach(key ->
                container.addEntry(Container.ChangeType.deleted, key, null, this::convertKey)
        );
        return container.isEmpty() ? null : container;
    }

    private Object setDiff(String storageName, Set<?> leftSet, Set<?> rightSet, int level) {
        Container container = Container.create(properties.getKeyType(storageName, level),
                properties.getValueType(storageName, level), properties.getSort(storageName, level),
                properties.getChangeType(storageName, level));

        rightSet.stream()
                .filter(item -> !leftSet.contains(item))
                .forEach(item -> container.addEntry(Container.ChangeType.added, null, item, this::convertKey));

        leftSet.stream()
                .filter(item -> !rightSet.contains(item))
                .forEach(item -> container.addEntry(Container.ChangeType.deleted, null, item, this::convertKey));

        return container.isEmpty() ? null : container;
    }

    private Object pojoDiff(String storageName, Object left, Object right, int level) {
        Map<String, Object> leftMap = jacksonConverter.convertPojo(left);
        Map<String, Object> rightMap = jacksonConverter.convertPojo(right);
        return mapDiff(storageName, leftMap, rightMap, level);
    }

    private String convertKey(Object key) {
        if (key == null) {
            return null;
        }
        if (key instanceof String s) {
            return s;
        }
        key = nonStringKeysConverter.apply(key);
        if (key instanceof String s) {
            return s;
        }
        if (key instanceof BigDecimal bigDecimal) {
            return bigDecimal.stripTrailingZeros().toPlainString();
        }
        return key.toString();
    }
}

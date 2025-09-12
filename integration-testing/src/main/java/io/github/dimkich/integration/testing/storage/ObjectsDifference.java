package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.storage.mapping.Container;
import io.github.dimkich.integration.testing.util.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonDifferenceCalculator;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ObjectsDifference {
    private static final RecursiveComparisonDifferenceCalculator compCalculator =
            new RecursiveComparisonDifferenceCalculator();
    private static final RecursiveComparisonConfiguration compConfig = new RecursiveComparisonConfiguration();

    private final StorageProperties properties;
    private final Function<Object, Object> nonStringKeysConverter = Function.identity();
    private final Function<Object, Map<String, Object>> pojoToMapConverter = PojoToMapUtil::mapWithReflectionFields;
    private final Function<Object, Boolean> simpleTypeDetector = o -> BeanUtils.isSimpleValueType(o.getClass());

    private String name;

    public Object getDifference(Object left, Object right) {
        return diff(left, right, 0);
    }

    private Object diff(Object left, Object right, int level) {
        if (right == null) {
            return null;
        }
        if ((left == null || simpleTypeDetector.apply(left)) && simpleTypeDetector.apply(right)) {
            return right;
        }
        if ((left == null || left instanceof Map<?, ?>) && right instanceof Map<?, ?>) {
            return mapDiff(left == null ? Map.of() : (Map<?, ?>) left, (Map<?, ?>) right, level);
        }
        if ((left == null || left instanceof Collection<?>) && right instanceof Collection<?>) {
            return collectionDiff(left == null ? List.of() : (Collection<?>) left, (Collection<?>) right, level);
        }
        return left == null ? right : pojoDiff(left, right, level);
    }

    private Object mapDiff(Map<?, ?> left, Map<?, ?> right, int level) {
        Container container = Container.create(properties.getKeyType(name, level), properties.getValueType(name, level),
                properties.getSort(name, level), properties.getChangeType(name, level));
        CollectionUtils.setsDifference(right.keySet(), left.keySet()).forEach(key -> {
            if (level == 0) {
                name = convertKey(key);
            }
            container.addEntry(Container.ChangeType.added, key, diff(null, right.get(key), level + 1), this::convertKey);
            if (level == 0) {
                name = null;
            }
        });
        CollectionUtils.setsIntersection(right.keySet(), left.keySet()).forEach(key -> {
            Object leftValue = left.get(key);
            Object rightValue = right.get(key);
            if (!isEquals(leftValue, rightValue)) {
                if (level == 0) {
                    name = convertKey(key);
                }
                container.addEntry(Container.ChangeType.changed, key, diff(leftValue, rightValue, level + 1), this::convertKey);
                if (level == 0) {
                    name = null;
                }
            }
        });
        CollectionUtils.setsDifference(left.keySet(), right.keySet()).forEach(key ->
                container.addEntry(Container.ChangeType.deleted, key, null, this::convertKey)
        );
        return container.isEmpty() ? null : container;
    }

    private Object collectionDiff(Collection<?> left, Collection<?> right, int level) {
        return mapDiff(collectionToMap(left), collectionToMap(right), level);
    }

    private Object pojoDiff(Object left, Object right, int level) {
        return mapDiff(pojoToMapConverter.apply(left), pojoToMapConverter.apply(right), level);
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

    private Map<Integer, Object> collectionToMap(Collection<?> collection) {
        int i = 0;
        Map<Integer, Object> map = new LinkedHashMap<>();
        for (Object item : collection) {
            map.put(i, item);
            i++;
        }
        return map;
    }

    @SneakyThrows
    private boolean isEquals(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            return o1 == o2;
        }
        if (o1.getClass() == o1.getClass().getMethod("equals", Object.class).getDeclaringClass()) {
            return Objects.equals(o1, o2);
        }
        return compCalculator.determineDifferences(o1, o2, compConfig).isEmpty();
    }
}

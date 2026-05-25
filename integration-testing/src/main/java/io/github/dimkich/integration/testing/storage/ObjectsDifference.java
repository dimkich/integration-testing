package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.date.time.PeriodDuration;
import io.github.dimkich.integration.testing.storage.mapping.Container;
import io.github.dimkich.integration.testing.util.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonDifferenceCalculator;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ObjectsDifference {
    private static final RecursiveComparisonDifferenceCalculator compCalculator =
            new RecursiveComparisonDifferenceCalculator();
    private static final RecursiveComparisonConfiguration compConfig = new RecursiveComparisonConfiguration();

    private final StorageProperties properties;
    private final JacksonConverter jacksonConverter;
    private final Function<Object, Object> nonStringKeysConverter = Function.identity();
    private final Function<Object, Boolean> simpleTypeDetector = o -> BeanUtils.isSimpleValueType(o.getClass())
            || o.getClass() == PeriodDuration.class || o.getClass() == byte[].class;

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
        if (right instanceof Set<?> rightSet) {
            return setDiff(left instanceof Set<?> ? (Set<?>) left : Set.of(), rightSet, level);
        }
        if ((left == null || left instanceof Collection<?>) && right instanceof Collection<?>) {
            return right;
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

    private Object setDiff(Set<?> leftSet, Set<?> rightSet, int level) {
        Container container = Container.create(properties.getKeyType(name, level), properties.getValueType(name, level),
                properties.getSort(name, level), properties.getChangeType(name, level));

        rightSet.stream()
                .filter(item -> !leftSet.contains(item))
                .forEach(item -> container.addEntry(Container.ChangeType.added, null, item, this::convertKey));

        leftSet.stream()
                .filter(item -> !rightSet.contains(item))
                .forEach(item -> container.addEntry(Container.ChangeType.deleted, null, item, this::convertKey));

        return container.isEmpty() ? null : container;
    }

    private Object pojoDiff(Object left, Object right, int level) {
        Map<String, Object> leftMap = jacksonConverter.convertPojo(left);
        Map<String, Object> rightMap = jacksonConverter.convertPojo(right);
        return mapDiff(leftMap, rightMap, level);
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

    @SneakyThrows
    private boolean isEquals(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            return o1 == o2;
        }
        if (simpleTypeDetector.apply(o1) && simpleTypeDetector.apply(o2)) {
            return Objects.equals(o1, o2);
        }
        return compCalculator.determineDifferences(o1, o2, compConfig).isEmpty();
    }
}

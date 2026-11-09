package io.github.dimkich.integration.testing.comparison;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Compares maps like AssertJ's recursive comparison does, except that keys are matched
 * with the configured equality predicates instead of {@link Object#equals(Object)}.
 * <p>
 * AssertJ always matches map keys by {@code equals}, even when a custom comparator is registered
 * for the key type, so maps with {@link java.math.BigDecimal} keys {@code 0.1} and {@code 0.10}
 * would be reported as different.
 * <p>
 * This is a known AssertJ limitation, see
 * <a href="https://github.com/assertj/assertj/issues/3835">assertj#3835</a>
 * ("RecursiveComparisonConfiguration is not taken into account for map keys"). It is fixed
 * in AssertJ 4.0.0-M2 and later, where map keys are compared with the configured
 * recursive comparison; after upgrading to that version this class can be removed.
 */
public class MapEquals implements BiPredicate<Map<?, ?>, Map<?, ?>> {
    private final BiPredicate<Object, Object> recursiveEquals;

    public MapEquals(BiPredicate<Object, Object> recursiveEquals) {
        this.recursiveEquals = recursiveEquals;
    }

    @Override
    public boolean test(Map<?, ?> actual, Map<?, ?> expected) {
        if (actual.size() != expected.size()) {
            return false;
        }
        if (hasSameKeys(actual, expected)) {
            for (Map.Entry<?, ?> actualEntry : actual.entrySet()) {
                if (!recursiveEquals.test(actualEntry.getValue(), expected.get(actualEntry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        List<Map.Entry<?, ?>> unmatched = new ArrayList<>(expected.entrySet());
        for (Map.Entry<?, ?> actualEntry : actual.entrySet()) {
            Map.Entry<?, ?> expectedEntry = removeEntryWithEqualKey(unmatched, actualEntry.getKey());
            if (expectedEntry == null || !recursiveEquals.test(actualEntry.getValue(), expectedEntry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether both maps contain the same keys according to their own lookup rules,
     * so that values can be compared by {@link Map#get(Object)} without scanning the entries.
     */
    private boolean hasSameKeys(Map<?, ?> actual, Map<?, ?> expected) {
        if (!actual.keySet().equals(expected.keySet())) {
            return false;
        }
        for (Object key : actual.keySet()) {
            if (!expected.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    private Map.Entry<?, ?> removeEntryWithEqualKey(List<Map.Entry<?, ?>> entries, Object key) {
        for (Iterator<Map.Entry<?, ?>> iterator = entries.iterator(); iterator.hasNext(); ) {
            Map.Entry<?, ?> entry = iterator.next();
            if (recursiveEquals.test(entry.getKey(), key)) {
                iterator.remove();
                return entry;
            }
        }
        return null;
    }
}

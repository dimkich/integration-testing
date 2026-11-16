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
 * <p>
 * The preset is an equality rule only: the diff engine leaves a map to {@code MapDiffRenderer}, so
 * the preset cannot hide metadata attributes or a changed map class on its own.
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
        for (Map.Entry<?, ?> actualEntry : actual.entrySet()) {
            Object expectedValue = expected.get(actualEntry.getKey());
            if (expectedValue == null && !expected.containsKey(actualEntry.getKey())) {
                return equalsByPredicate(actual, expected);
            }
            if (!recursiveEquals.test(actualEntry.getValue(), expectedValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Matches keys by the configured predicates when they are not equal as map keys. Sizes are
     * already known to be equal, so a match of every key means that the key sets are equal.
     */
    private boolean equalsByPredicate(Map<?, ?> actual, Map<?, ?> expected) {
        List<Map.Entry<?, ?>> unmatched = new ArrayList<>(expected.entrySet());
        for (Map.Entry<?, ?> actualEntry : actual.entrySet()) {
            Map.Entry<?, ?> expectedEntry = removeEntryWithEqualKey(unmatched, actualEntry.getKey());
            if (expectedEntry == null || !recursiveEquals.test(actualEntry.getValue(), expectedEntry.getValue())) {
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

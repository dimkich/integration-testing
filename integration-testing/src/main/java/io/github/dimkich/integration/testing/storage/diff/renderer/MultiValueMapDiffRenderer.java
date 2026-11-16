package io.github.dimkich.integration.testing.storage.diff.renderer;

import io.github.dimkich.integration.testing.storage.diff.ObjectsDifference;
import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import io.github.dimkich.integration.testing.storage.diff.model.DiffKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffObjectValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Renders a multi-value map as repeated elements of the same name.
 * <p>
 * A key identifies its values, so a changed map contains only changes: a key that appears or
 * disappears is written with all its values as inserts or deletes, and for a key present on both
 * sides a new value produces an {@code insert} while a missing value produces a {@code delete}.
 * The whole map is written only when it appears, because the body of an insert is plain data.
 * <p>
 * The values of one key are a sequence without identifiers of their own, so their order is not a
 * part of the difference: the same values in another order are the same field. Values are matched
 * by the configured equality rules, so a registered predicate works for the values too.
 * <p>
 * When the body itself did not change, the writer leaves the body out, so a change of the
 * metadata attributes only produces an element with attributes and without a body, as for a map or
 * a set.
 */
public class MultiValueMapDiffRenderer implements DiffRenderer<MultiValueMap<?, ?>> {

    @Override
    public DiffObjectValue render(Object before, MultiValueMap<?, ?> after, ObjectsDifference difference) {
        if (before == null) {
            return new DiffObjectValue(snapshot(difference, after));
        }
        Map<?, ?> beforeMap = before instanceof Map<?, ?> map ? map : Map.of();
        Map<DiffKey, Object> beforeFields = DiffFields.collect(difference, beforeMap);
        List<DiffEntry> entries = new ArrayList<>();
        difference.forEachField(after, false, (key, afterValues) -> {
            Object beforeValue = beforeFields.remove(key);
            List<Object> removed = beforeValue == null
                    ? new ArrayList<>()
                    : new ArrayList<>(values(DiffFields.unwrap(beforeValue)));
            for (Object value : values(afterValues)) {
                if (!removeEqual(removed, value, difference)) {
                    entries.add(difference.insert(key, value));
                }
            }
            for (Object value : removed) {
                entries.add(difference.deleteValue(key, value));
            }
        });
        for (Map.Entry<DiffKey, Object> field : beforeFields.entrySet()) {
            for (Object value : values(DiffFields.unwrap(field.getValue()))) {
                entries.add(difference.deleteValue(field.getKey(), value));
            }
        }
        ContainerOrder.sortIfUnordered(entries, beforeMap, after);
        return entries.isEmpty() ? null : new DiffObjectValue(entries);
    }

    /**
     * A multi-value map is written as differences, so the writer omits the body of an unchanged
     * value; the element keeps only its attributes, as a map or a set does.
     */
    @Override
    public DiffValue renderUnchanged(MultiValueMap<?, ?> body, ObjectsDifference difference) {
        return null;
    }

    /**
     * Whole content of a map as plain fields: the body of an insert has no operations.
     */
    private List<DiffEntry> snapshot(ObjectsDifference difference, MultiValueMap<?, ?> map) {
        List<DiffEntry> entries = new ArrayList<>();
        difference.forEachField(map, false, (key, afterValues) -> {
            for (Object value : values(afterValues)) {
                entries.add(difference.update(key, null, value));
            }
        });
        ContainerOrder.sortIfUnordered(entries, map);
        return entries;
    }

    /**
     * Values of a map field. A multi-value map always holds sequences, but the body before the test
     * may be a plain map whose field is a single value.
     */
    private static List<?> values(Object value) {
        if (value instanceof List<?> values) {
            return values;
        }
        return value == null ? List.of() : Collections.singletonList(value);
    }

    /**
     * Removes the first value equal to the given one and reports whether the value was found.
     */
    private static boolean removeEqual(List<Object> values, Object value, ObjectsDifference difference) {
        for (Iterator<Object> iterator = values.iterator(); iterator.hasNext(); ) {
            if (difference.isEquals(iterator.next(), value)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }
}

package io.github.dimkich.integration.testing.storage.diff.renderer;

import io.github.dimkich.integration.testing.storage.diff.ObjectsDifference;
import io.github.dimkich.integration.testing.storage.diff.model.DiffCollectionValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import io.github.dimkich.integration.testing.storage.diff.model.DiffOperation;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Renders a set element by element: an element has no identifier of its own, so added and removed
 * elements are written with operations. An ordered set keeps its order, a hash based one is
 * sorted by the rendered element.
 * <p>
 * Elements are matched by the equality of the set itself — {@link Object#equals(Object)} and
 * {@link Object#hashCode()} for a hash based set, the comparator for a sorted set — not by the
 * equality predicates registered with {@code addEqualsForType}. A set is expected to hold values
 * that compare well by their own equality; a set of {@code BigDecimal}, {@code byte[]} or another
 * type with a non-standard equality needs a custom renderer for the set type.
 */
public class SetDiffRenderer implements DiffRenderer<Set<?>> {

    @Override
    public DiffCollectionValue render(Object before, Set<?> after, ObjectsDifference difference) {
        if (before == null) {
            return new DiffCollectionValue(difference.entries(after, null).toList());
        }
        List<DiffEntry> diff = diff(difference,
                before instanceof Set<?> beforeSet ? beforeSet : Set.of(), after);
        return diff.isEmpty() ? null : new DiffCollectionValue(diff);
    }

    @Override
    public DiffValue renderUnchanged(Set<?> body, ObjectsDifference difference) {
        return null;
    }

    /**
     * Difference of two sets: added elements first, then removed ones.
     */
    private List<DiffEntry> diff(ObjectsDifference difference, Set<?> before, Set<?> after) {
        return Stream.concat(
                difference.entries(after, DiffOperation.INSERT, item -> !before.contains(item)),
                difference.entries(before, DiffOperation.DELETE, item -> !after.contains(item)))
                .toList();
    }
}

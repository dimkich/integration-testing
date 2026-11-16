package io.github.dimkich.integration.testing.storage.diff.renderer;

import io.github.dimkich.integration.testing.storage.diff.ObjectsDifference;
import io.github.dimkich.integration.testing.storage.diff.model.DiffCollectionValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;

/**
 * Renders an array or a collection as a whole: elements of a sequence have no identifier to diff
 * them by, so a changed sequence is written as a snapshot of its new state.
 *
 * @param <T> body type after the test
 */
public class SnapshotDiffRenderer<T> implements DiffRenderer<T> {

    @Override
    public DiffValue render(Object before, T after, ObjectsDifference difference) {
        if (before != null && difference.isEquals(before, after)) {
            return null;
        }
        return new DiffCollectionValue(difference.entries(after, null).toList());
    }
}

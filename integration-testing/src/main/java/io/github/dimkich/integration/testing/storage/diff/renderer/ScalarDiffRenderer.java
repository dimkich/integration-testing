package io.github.dimkich.integration.testing.storage.diff.renderer;

import io.github.dimkich.integration.testing.storage.diff.ObjectsDifference;
import io.github.dimkich.integration.testing.storage.diff.format.ValueFormat;
import io.github.dimkich.integration.testing.storage.diff.model.DiffScalarValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import lombok.RequiredArgsConstructor;

/**
 * Renders a scalar as text. A scalar has no fields to diff, so a changed value is always written
 * as a snapshot of the new state.
 */
@RequiredArgsConstructor
public class ScalarDiffRenderer implements DiffRenderer<Object> {
    private final ValueFormat valueFormat;

    @Override
    public DiffValue render(Object before, Object after, ObjectsDifference difference) {
        if (before != null && difference.isEquals(before, after)) {
            return null;
        }
        return new DiffScalarValue(valueFormat.text(after));
    }
}

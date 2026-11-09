package io.github.dimkich.integration.testing.comparison;

import java.time.temporal.Temporal;
import java.util.function.BiPredicate;

/**
 * Compares {@link Temporal} values with {@code equals}.
 */
public class TemporalEquals implements BiPredicate<Temporal, Temporal> {
    @Override
    public boolean test(Temporal actual, Temporal expected) {
        return actual.equals(expected);
    }
}

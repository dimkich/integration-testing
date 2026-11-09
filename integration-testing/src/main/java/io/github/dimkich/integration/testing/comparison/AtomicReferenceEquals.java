package io.github.dimkich.integration.testing.comparison;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

/**
 * Compares {@link AtomicReference} values by their references.
 */
public class AtomicReferenceEquals implements BiPredicate<AtomicReference<?>, AtomicReference<?>> {
    private final BiPredicate<Object, Object> recursiveEquals;

    public AtomicReferenceEquals(BiPredicate<Object, Object> recursiveEquals) {
        this.recursiveEquals = recursiveEquals;
    }

    @Override
    public boolean test(AtomicReference<?> actual, AtomicReference<?> expected) {
        return recursiveEquals.test(actual.get(), expected.get());
    }
}

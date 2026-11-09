package io.github.dimkich.integration.testing.comparison;

import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.function.BiPredicate;

/**
 * Compares {@link AtomicMarkableReference} values by their references, ignoring the mark.
 */
@SuppressWarnings("SpellCheckingInspection")
public class AtomicMarkableReferenceEquals implements BiPredicate<AtomicMarkableReference<?>, AtomicMarkableReference<?>> {
    private final BiPredicate<Object, Object> recursiveEquals;

    public AtomicMarkableReferenceEquals(BiPredicate<Object, Object> recursiveEquals) {
        this.recursiveEquals = recursiveEquals;
    }

    @Override
    public boolean test(AtomicMarkableReference<?> actual, AtomicMarkableReference<?> expected) {
        return recursiveEquals.test(actual.getReference(), expected.getReference());
    }
}

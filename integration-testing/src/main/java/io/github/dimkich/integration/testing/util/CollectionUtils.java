package io.github.dimkich.integration.testing.util;

import java.util.Set;
import java.util.stream.Stream;

public class CollectionUtils {
    public static <E> Stream<E> setsDifference(Set<E> set1, Set<?> set2) {
        return set1.stream().filter(e -> !set2.contains(e));
    }

    public static <E> Stream<E> setsIntersection(final Set<E> set1, final Set<?> set2) {
        return set1.stream().filter(set2::contains);
    }
}

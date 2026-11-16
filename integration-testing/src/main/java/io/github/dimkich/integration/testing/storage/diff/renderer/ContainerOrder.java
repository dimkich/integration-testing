package io.github.dimkich.integration.testing.storage.diff.renderer;

import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import io.github.dimkich.integration.testing.util.FastPath;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Order of the fields and elements in the output: a container that keeps an order of its own is
 * written in its iteration order, a plain hash based one is sorted by the rendered key.
 * <p>
 * The policy is public because a custom renderer of a map-like body needs the same ordering as the
 * built-in renderers. The order of a collection is used by {@code ObjectsDifference.entries}, which
 * sorts a hash based collection by the rendered element rather than by its key.
 * <p>
 * The decisions themselves are answered by {@link FastPath}; this class keeps the public API and
 * the sorting rule.
 */
public final class ContainerOrder {
    private static final Comparator<DiffEntry> BY_KEY = Comparator.comparing(DiffEntry::key);

    private ContainerOrder() {
    }

    /**
     * Sorts the fields by key when the map has no order of its own.
     *
     * @param entries fields to order
     * @param map     map the fields were built from
     */
    public static void sortIfUnordered(List<DiffEntry> entries, Map<?, ?> map) {
        if (!FastPath.isOrdered(map)) {
            entries.sort(BY_KEY);
        }
    }

    /**
     * Sorts the fields by key when at least one of the maps has no order of its own.
     *
     * @param entries fields to order
     * @param first   first source map
     * @param second  second source map
     */
    public static void sortIfUnordered(List<DiffEntry> entries, Map<?, ?> first, Map<?, ?> second) {
        if (!FastPath.isOrdered(first) || !FastPath.isOrdered(second)) {
            entries.sort(BY_KEY);
        }
    }

    /**
     * Reports whether a map keeps an order of its own.
     *
     * @param map map to inspect
     * @return {@code true} when the iteration order of the map is meaningful
     * @see FastPath#isOrdered(Map)
     */
    public static boolean isOrdered(Map<?, ?> map) {
        return FastPath.isOrdered(map);
    }

    /**
     * Reports whether a collection keeps an order of its own.
     *
     * @param collection collection to inspect
     * @return {@code true} when the iteration order of the collection is meaningful
     * @see FastPath#isOrdered(Collection)
     */
    public static boolean isOrdered(Collection<?> collection) {
        return FastPath.isOrdered(collection);
    }
}

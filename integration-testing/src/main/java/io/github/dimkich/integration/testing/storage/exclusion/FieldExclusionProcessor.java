package io.github.dimkich.integration.testing.storage.exclusion;

import io.github.dimkich.integration.testing.storage.pojo.PojoAccessorService;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compiles dot-separated field paths into {@link FieldExclusionTree} instances for use in
 * excluding or clearing fields from POJOs, maps, collections, and arrays during storage processing.
 * <p>
 * Compiled trees are cached by path collection for reuse. A shared default tree is returned when
 * {@code paths} is null or empty.
 */
@RequiredArgsConstructor
public class FieldExclusionProcessor {
    private final static Map<Collection<String>, FieldExclusionTree> treeCache = new ConcurrentHashMap<>();
    private final static FieldExclusionTree defaultTree = new FieldExclusionTree(null);

    private final PojoAccessorService pojoAccessorService;

    /**
     * Compiles the given field paths into a {@link FieldExclusionTree}.
     *
     * @param paths dot-separated paths (e.g. {@code "user.address.city"}), or null/empty
     * @return a tree that can {@link FieldExclusionTree#process(Object) process} objects to
     * exclude the specified fields; returns a shared default tree when paths is null or empty
     */
    public FieldExclusionTree compile(Collection<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return defaultTree;
        }
        return treeCache.computeIfAbsent(paths, (p) -> {
            FieldExclusionTree root = new FieldExclusionTree(pojoAccessorService);
            for (String path : paths) {
                root.addPath(path);
            }
            return root;
        });
    }
}

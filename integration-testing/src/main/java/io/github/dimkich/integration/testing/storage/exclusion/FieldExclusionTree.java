package io.github.dimkich.integration.testing.storage.exclusion;

import io.github.dimkich.integration.testing.storage.pojo.PojoAccessor;
import io.github.dimkich.integration.testing.storage.pojo.PojoAccessorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * A tree structure representing dot-separated field paths for exclusion.
 * <p>
 * Each node corresponds to a path segment. Leaf nodes represent complete paths;
 * traversing the tree and processing an object will remove or clear the fields
 * that match the configured paths. Supports POJOs, {@link Map}s, {@link Iterable}s,
 * and arrays.
 * <p>
 * Instances are typically obtained via {@link FieldExclusionProcessor#compile(java.util.Collection)}.
 *
 * @see FieldExclusionProcessor
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class FieldExclusionTree {
    private final PojoAccessorService pojoAccessorService;
    private Map<String, FieldExclusionTree> children;

    /**
     * Adds a dot-separated field path to this tree (e.g. {@code "user.address.city"}).
     * Creates child nodes as needed. Null or empty paths are ignored.
     *
     * @param path dot-separated field path to add, or null/empty
     */
    void addPath(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        if (this.children == null) {
            this.children = new HashMap<>();
        }
        int dotIdx = path.indexOf('.');
        if (dotIdx == -1) {
            this.children.put(path, new FieldExclusionTree(pojoAccessorService));
        } else {
            String head = path.substring(0, dotIdx);
            String tail = path.substring(dotIdx + 1);
            this.children.computeIfAbsent(head, k -> new FieldExclusionTree(pojoAccessorService)).addPath(tail);
        }
    }

    /**
     * Processes the given item to exclude or clear fields according to this tree's paths.
     * Handles POJOs (via {@link PojoAccessorService}), {@link Map}s, {@link Iterable}s,
     * and arrays. Null items, empty trees, and unmatched types are skipped.
     *
     * @param item the object to process (POJO, Map, Iterable, or array)
     */
    @SuppressWarnings("unchecked")
    public void process(Object item) {
        if (item == null || isEmpty()) {
            return;
        }
        if (item instanceof Map<?, ?> map) {
            processMap((Map<Object, Object>) map);
        } else if (item instanceof Iterable<?> it) {
            it.forEach(this::process);
        } else if (item.getClass().isArray()) {
            Object[] arr = (Object[]) item;
            for (Object o : arr) {
                process(o);
            }
        } else {
            processPojo(item);
        }
    }

    private void processMap(Map<Object, Object> map) {
        if (children == null) {
            return;
        }
        children.forEach((key, childNode) -> {
            if (map.containsKey(key)) {
                if (childNode.isEmpty()) {
                    map.remove(key);
                } else {
                    childNode.process(map.get(key));
                }
            }
        });
    }

    private void processPojo(Object pojo) {
        if (children == null) {
            return;
        }
        PojoAccessor accessor = pojoAccessorService.forBean(pojo);
        children.forEach((fieldName, childNode) -> {
            if (childNode.isEmpty()) {
                accessor.setPropertyValue(fieldName, null);
            } else {
                childNode.process(accessor.getPropertyValue(fieldName));
            }
        });
    }

    private boolean isEmpty() {
        return children == null || children.isEmpty();
    }
}
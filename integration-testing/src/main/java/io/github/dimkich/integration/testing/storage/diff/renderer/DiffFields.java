package io.github.dimkich.integration.testing.storage.diff.renderer;

import io.github.dimkich.integration.testing.storage.diff.ObjectsDifference;
import io.github.dimkich.integration.testing.storage.diff.model.DiffKey;
import io.github.dimkich.integration.testing.util.FastPath;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rendered fields of a map before the test, with an explicitly null value kept apart from an
 * absent key. The map is built once, so a renderer can remove a field while iterating the fields
 * after the test and tell "the key was absent" from "the key held null".
 */
final class DiffFields {
    private static final Object NULL = new Object();

    private DiffFields() {
    }

    /**
     * @param difference difference engine of the current build
     * @param map        map to render
     * @return fields by rendered key, in the iteration order of the map
     */
    static Map<DiffKey, Object> collect(ObjectsDifference difference, Map<?, ?> map) {
        Map<DiffKey, Object> fields = new LinkedHashMap<>(FastPath.hashCapacity(map.size()));
        difference.forEachField(map, false,
                (key, value) -> fields.put(key, value == null ? NULL : value));
        return fields;
    }

    /**
     * @param value value stored by {@link #collect}
     * @return the original value, {@code null} when the field held an explicit null
     */
    static Object unwrap(Object value) {
        return value == NULL ? null : value;
    }
}

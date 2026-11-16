package io.github.dimkich.integration.testing.storage.diff.renderer;

import io.github.dimkich.integration.testing.storage.diff.ObjectsDifference;
import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import io.github.dimkich.integration.testing.storage.diff.model.DiffKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffObjectValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders a map as named fields: a new map is a snapshot, a changed map contains only changed
 * fields.
 */
public class MapDiffRenderer implements DiffRenderer<Map<?, ?>> {

    @Override
    public DiffObjectValue render(Object before, Map<?, ?> after, ObjectsDifference difference) {
        if (before == null) {
            return new DiffObjectValue(snapshotEntries(difference, after));
        }
        List<DiffEntry> fields = fields(difference,
                before instanceof Map<?, ?> beforeMap ? beforeMap : Map.of(), after);
        return fields.isEmpty() ? null : new DiffObjectValue(fields);
    }

    @Override
    public DiffValue renderUnchanged(Map<?, ?> body, ObjectsDifference difference) {
        return null;
    }

    /**
     * Whole content of a map without change markers.
     */
    private List<DiffEntry> snapshotEntries(ObjectsDifference difference, Map<?, ?> map) {
        List<DiffEntry> entries = new ArrayList<>(map.size());
        difference.forEachField(map, false,
                (key, value) -> entries.add(difference.update(key, null, value)));
        ContainerOrder.sortIfUnordered(entries, map);
        return entries;
    }

    /**
     * Difference of the fields of one object or map: a changed field keeps its own tag.
     */
    private List<DiffEntry> fields(ObjectsDifference difference, Map<?, ?> before, Map<?, ?> after) {
        Map<DiffKey, Object> beforeFields = DiffFields.collect(difference, before);

        List<DiffEntry> entries = new ArrayList<>();
        difference.forEachField(after, false, (key, afterValue) -> {
            Object beforeValue = beforeFields.remove(key);
            if (beforeValue == null) {
                entries.add(difference.insert(key, afterValue));
                return;
            }
            beforeValue = DiffFields.unwrap(beforeValue);
            if (afterValue == null) {
                if (beforeValue != null) {
                    entries.add(difference.update(key, beforeValue, null));
                }
                return;
            }
            DiffEntry changed = difference.update(key, beforeValue, afterValue);
            if (changed != null) {
                entries.add(changed);
            }
        });
        for (Map.Entry<DiffKey, Object> removed : beforeFields.entrySet()) {
            entries.add(difference.deleteElement(removed.getKey(), DiffFields.unwrap(removed.getValue())));
        }
        ContainerOrder.sortIfUnordered(entries, before, after);
        return entries;
    }
}

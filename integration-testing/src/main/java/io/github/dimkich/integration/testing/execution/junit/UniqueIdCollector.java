package io.github.dimkich.integration.testing.execution.junit;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.UniqueIdSelector;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(of = {"id"})
class UniqueIdCollector {
    private UniqueId id;
    private List<Integer> indexes = new ArrayList<>();

    public UniqueIdCollector(UniqueIdSelector selector) {
        List<UniqueId.Segment> segments = selector.getUniqueId().getSegments();
        id = UniqueId.root(segments.get(0).getType(), segments.get(0).getValue());
        for (int i = 1; i < segments.size(); i++) {
            if (i < 3) {
                id = id.append(segments.get(i));
            } else {
                indexes.add(Integer.parseInt(segments.get(i).getValue().substring(1)) - 1);
            }
        }
    }

    public UniqueIdCollector max(UniqueIdCollector uniqueIdCollector) {
        if (uniqueIdCollector == null) {
            return this;
        }
        int min = Math.min(indexes.size(), uniqueIdCollector.indexes.size());
        for (int i = 0; i < min; i++) {
            if (indexes.get(i) > uniqueIdCollector.indexes.get(i)) {
                return this;
            } else if (indexes.get(i) < uniqueIdCollector.indexes.get(i)) {
                return uniqueIdCollector;
            }
        }
        return indexes.size() < uniqueIdCollector.indexes.size() ? this : uniqueIdCollector;
    }
}

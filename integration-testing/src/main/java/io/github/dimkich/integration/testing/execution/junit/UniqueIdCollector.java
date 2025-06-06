package io.github.dimkich.integration.testing.execution.junit;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.UniqueIdSelector;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(of = {"segments"})
class UniqueIdCollector {
    private UniqueId id;
    private List<UniqueId.Segment> segments = new ArrayList<>();
    private List<Integer> indexes = new ArrayList<>();

    public UniqueIdCollector(UniqueIdSelector selector) {
        id = selector.getUniqueId();
        List<UniqueId.Segment> segments = selector.getUniqueId().getSegments();
        for (int i = 0; i < segments.size(); i++) {
            if (i < 3) {
                this.segments.add(segments.get(i));
            } else {
                indexes.add(Integer.valueOf(segments.get(i).getValue().substring(1)));
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

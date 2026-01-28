package io.github.dimkich.integration.testing.execution.junit;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.UniqueIdSelector;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper that normalizes a {@link UniqueId} from a {@link UniqueIdSelector}
 * and collects trailing numeric index segments.
 * <p>
 * The first three segments of the original {@link UniqueId} (engine, class,
 * method or container) are preserved in {@link #id}. Remaining segments are
 * expected to be of form {@code "sN"} and are converted to zero-based integer
 * indexes stored in {@link #indexes}.
 */
@Data
@EqualsAndHashCode(of = {"id"})
class UniqueIdCollector {
    private UniqueId id;
    private List<Integer> indexes = new ArrayList<>();

    /**
     * Creates a collector from the given selector by splitting its
     * {@link UniqueId} into a normalized root id and a list of numeric indexes.
     *
     * @param selector selector that provides the {@link UniqueId} to analyze
     */
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

    /**
     * Returns the collector that represents the lexicographically greatest
     * index sequence between this instance and the given one.
     * <p>
     * Comparison is performed element-by-element on {@link #indexes}. If all
     * compared elements are equal but one list is longer, the longer list is
     * considered greater.
     *
     * @param uniqueIdCollector another collector to compare with; may be {@code null}
     * @return {@code this} if it is greater or equal, the given collector if it is greater,
     * or {@code this} when {@code uniqueIdCollector} is {@code null}
     */
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

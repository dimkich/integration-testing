package io.github.dimkich.integration.testing.storage.dto;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * Sorted map with a reverse order of keys: the difference engine writes a sorted map in its own
 * order, which is the reverse of the natural one here.
 */
public class DescendingTreeMap extends TreeMap<String, Object> {
    public DescendingTreeMap() {
        super(Comparator.reverseOrder());
    }
}

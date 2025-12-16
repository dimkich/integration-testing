package io.github.dimkich.integration.testing.initialization;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A segmented list that maintains elements divided into logical segments, each with associated metadata.
 * This data structure allows grouping of elements while maintaining their sequential order,
 * with the ability to retrieve elements by segment and access segment-specific metadata.
 *
 * @param <T> the type of elements stored in the list
 * @param <D> the type of metadata associated with each segment
 */
public class SegmentedList<T, D> {
    /**
     * The underlying list containing all elements across all segments in sequential order.
     */
    @Getter
    private final List<T> elements = new ArrayList<>();

    /**
     * List of ending indices for each segment. Each index represents the exclusive end position
     * (0-based) of a segment in the elements list. For segment i, the elements range from:
     * - start = 0 (if i = 0) or segmentEnds.get(i - 1) (if i > 0)
     * - end = segmentEnds.get(i) (exclusive)
     */
    private final List<Integer> segmentEnds = new ArrayList<>();

    /**
     * List of metadata associated with each segment, parallel to segmentEnds list.
     * segmentData.get(i) contains the metadata for the i-th segment.
     */
    private final List<D> segmentData = new ArrayList<>();

    /**
     * Finalizes the current segment and associates it with the provided metadata.
     * This marks the end of the current segment; subsequent added elements will belong to a new segment.
     *
     * @param data the metadata to associate with the completed segment
     * @throws IllegalStateException if called when no elements have been added since the last segment
     */
    public void finishSegment(D data) {
        segmentEnds.add(elements.size());
        segmentData.add(data);
    }

    /**
     * Adds a single element to the current segment.
     *
     * @param element the element to add to the current segment
     */
    public void add(T element) {
        elements.add(element);
    }

    /**
     * Adds all elements from the specified collection to the current segment.
     *
     * @param elements the collection of elements to add to the current segment
     */
    public void addAll(Collection<T> elements) {
        this.elements.addAll(elements);
    }

    /**
     * Checks if the list contains no elements across all segments.
     *
     * @return true if the list has no elements, false otherwise
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * Removes all elements and segment information from this segmented list.
     * After calling this method, the list will be empty and have no segments.
     */
    public void clear() {
        elements.clear();
        segmentEnds.clear();
        segmentData.clear();
    }

    /**
     * Returns the metadata associated with the specified segment.
     *
     * @param segmentIndex the index of the segment (0-based)
     * @return the metadata associated with the segment
     * @throws IndexOutOfBoundsException if segmentIndex is out of range
     *                                   (segmentIndex < 0 || segmentIndex >= getSegmentCount())
     */
    public D getSegmentData(int segmentIndex) {
        return segmentData.get(segmentIndex);
    }

    /**
     * Returns a view of the elements in the specified segment.
     * The returned list is backed by the internal list, so changes to the internal list
     * that affect this segment will be reflected in the returned sublist.
     *
     * @param segmentIndex the index of the segment (0-based)
     * @return a list view of the elements in the specified segment
     * @throws IndexOutOfBoundsException if segmentIndex is out of range
     *                                   (segmentIndex < 0 || segmentIndex >= getSegmentCount())
     */
    public List<T> getSegment(int segmentIndex) {
        int start = segmentIndex == 0 ? 0 : segmentEnds.get(segmentIndex - 1);
        int end = segmentEnds.get(segmentIndex);
        return elements.subList(start, end);
    }

    /**
     * Returns the number of segments in this segmented list.
     * Note: This count includes only completed segments (segments that have been finalized
     * with finishSegment()). Elements added after the last call to finishSegment() belong
     * to an incomplete current segment and are not counted.
     *
     * @return the number of completed segments
     */
    public int getSegmentCount() {
        return segmentEnds.size();
    }
}
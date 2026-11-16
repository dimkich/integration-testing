package io.github.dimkich.integration.testing.storage.diff;

import io.github.dimkich.integration.testing.comparison.ObjectComparisonService;
import io.github.dimkich.integration.testing.storage.diff.format.ValueFormat;
import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import io.github.dimkich.integration.testing.storage.diff.model.DiffKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffObjectValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffOperation;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresentation;
import io.github.dimkich.integration.testing.util.FastPath;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Builds the structural difference between two snapshots of a storage.
 * <p>
 * The result is a list of record entries, each with an operation, an identifier and a body. The
 * body keeps the same shape at every level: named fields for objects and maps, unnamed elements
 * for lists and sets. Values that appear or disappear are rendered as a snapshot. Every element
 * gets an operation at construction, so the model never contains a missing operation; the writer
 * derives the operation tag from the context and writes a changed field under its own name.
 * <p>
 * A value is presented by a {@link io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter}:
 * the presenter supplies the type, the metadata attributes and the raw body. The presentation
 * chain is resolved by {@link DiffPresentationService}, so the presenter of the body's own type is
 * applied too; the difference of the final body is built by a {@code DiffRenderer} selected by the
 * body type through {@link DiffRenderService}. The engine does not know the concrete shapes of the
 * data, and a presenter never builds a difference itself.
 * <p>
 * The renderer is the source of truth for "changed" of the rendered body: a recursive renderer
 * returns {@code null} when the rendered bodies are equal, so the engine does not run an
 * additional recursive comparison over the object graph. A value also changes when its presenter
 * type or the presence of its body differs, even if the rendered bodies are equal. A
 * user-registered equality predicate is applied to the raw value before its presenter reshapes it
 * and decides equality on its own, because the predicate is registered for the value type while
 * the renderer sees the projected body. A framework preset is only an equality rule for the
 * renderer, so it cannot hide attributes or a type change.
 * <p>
 * Fields follow the order of the source: collections and maps that keep an order of their own are
 * written in their iteration order, plain hash based ones are sorted by the rendered key or
 * element. Records follow the same rule. Without this rule the expected test data would
 * depend on the hash order of the application collections.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ObjectsDifference {
    private final DiffPresentationService presentations;
    private final DiffRenderService renderService;
    private final ValueFormat valueFormat;
    private final ObjectComparisonService comparisons;

    /**
     * Computes the difference between two snapshots of one storage.
     *
     * @param before snapshot before the test, may be {@code null} for an empty snapshot
     * @param after  snapshot after the test, may be {@code null} for an empty snapshot
     * @return record entries in the output order of the snapshot, empty when nothing changed
     */
    public List<DiffEntry> getDifference(Map<?, ?> before, Map<?, ?> after) {
        DiffValue rendered = renderService.render(before == null ? Map.of() : before,
                after == null ? Map.of() : after, this);
        if (rendered == null) {
            return List.of();
        }
        if (!(rendered instanceof DiffObjectValue object)) {
            throw new IllegalStateException("The renderer of a storage snapshot must produce "
                    + "an object body, got " + rendered.getClass().getSimpleName());
        }
        return object.fields();
    }

    /**
     * Compares two values with the configured equality rules.
     *
     * @param before value before the test
     * @param after  value after the test
     * @return {@code true} when the values are equal
     */
    public boolean isEquals(Object before, Object after) {
        return comparisons.isEquals(before, after);
    }

    /**
     * Builds an element that appeared after the test. The body is a snapshot of the whole value.
     *
     * @param key   identifier of the element within its parent
     * @param after value after the test, {@code null} for an explicitly null value
     * @return difference element
     */
    public DiffEntry insert(DiffKey key, Object after) {
        return body(DiffOperation.INSERT, key, after);
    }

    /**
     * Builds an element that changed. The body contains only the changes.
     *
     * @param key    identifier of the element within its parent
     * @param before value before the test, may be {@code null} when the previous state is unknown
     * @param after  value after the test, {@code null} for an explicitly null value
     * @return changed element, or {@code null} when the value did not change
     */
    public DiffEntry update(DiffKey key, Object before, Object after) {
        if (after == null) {
            return DiffEntry.nil(DiffOperation.UPDATE, key);
        }
        if (renderService.isPlainScalar(after.getClass())
                && (before == null || before.getClass() == after.getClass())) {
            if (before != null && comparisons.isEquals(before, after)) {
                return null;
            }
            return renderService.scalarEntry(DiffOperation.UPDATE, key, after);
        }
        return build(DiffOperation.UPDATE, key, before, after);
    }

    /**
     * Builds the removal of a keyed element. A removed record or field is written without a body:
     * only the key and the semantic type of the removed value appear.
     *
     * @param key    identifier of the element within its parent
     * @param before removed value, may be {@code null}
     * @return difference element
     */
    public DiffEntry deleteElement(DiffKey key, Object before) {
        return new DiffEntry(DiffOperation.DELETE, key, presentations.type(before, valueFormat),
                DiffAttributes.EMPTY, null);
    }

    /**
     * Builds the removal of one value of a repeated multi-value map field. The removed value is
     * written as a snapshot body, because the key identifies the field rather than the value.
     *
     * @param key   identifier of the field within its parent
     * @param value removed value, may be {@code null}
     * @return difference element
     */
    public DiffEntry deleteValue(DiffKey key, Object value) {
        return body(DiffOperation.DELETE, key, value);
    }

    /**
     * Builds an element that exists after the test. An absent after value is handled by the public
     * methods, so the body is always present here. The render service resolves the presentations
     * into an entry or into "no entry".
     */
    private DiffEntry build(DiffOperation operation, DiffKey key, Object before, Object after) {
        if (comparisons.hasCustomEquals(after.getClass()) && comparisons.isEquals(before, after)) {
            return null;
        }
        DiffPresentation beforePresentation = presentations.presentValue(before);
        DiffPresentation afterPresentation = presentations.presentValue(after);
        return renderService.renderEntry(operation, key, beforePresentation, afterPresentation, this);
    }

    /**
     * Renders a value as a body snapshot under the given operation, or as an explicitly null body.
     */
    private DiffEntry body(DiffOperation operation, DiffKey key, Object value) {
        if (value == null) {
            return DiffEntry.nil(operation, key);
        }
        if (renderService.isPlainScalar(value.getClass())) {
            return renderService.scalarEntry(operation, key, value);
        }
        return build(operation, key, null, value);
    }

    /**
     * Streams the entries of a map with rendered keys, in the iteration order of the map.
     * <p>
     * Two keys that render to the same identifier are reported instead of silently merging
     * entries. The source map is scanned a second time only when a collision is found.
     *
     * @param map       source map
     * @param skipNulls {@code true} to treat a {@code null} value as an absent entry and skip it
     *                  instead of writing it as a present entry with an empty value
     * @param action    consumer of a rendered key and its value
     */
    public void forEachField(Map<?, ?> map, boolean skipNulls, BiConsumer<DiffKey, Object> action) {
        Set<DiffKey> seen = new HashSet<>(FastPath.hashCapacity(map.size()));
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (skipNulls && value == null) {
                continue;
            }
            DiffKey key = renderService.renderKey(entry.getKey(), this);
            if (!seen.add(key)) {
                throw collision(map, skipNulls, key, entry.getKey());
            }
            action.accept(key, value);
        }
    }

    /**
     * Elements of an array or a collection as difference entries in output order.
     * <p>
     * An array and a collection that has an order of its own keep their order; a plain hash based
     * collection is filtered before rendering, rendered once and sorted by the full element entry,
     * so an unchanged element is not rendered at all.
     *
     * @param items     array or collection to render
     * @param operation operation of each entry, {@code null} means the default {@code update}
     * @return entries in output order
     */
    public Stream<DiffEntry> entries(Object items, DiffOperation operation) {
        return entries(items, operation, item -> true);
    }

    /**
     * Elements of an array or a collection as difference entries in output order, keeping only
     * the elements accepted by the filter. The filter is applied before rendering, so a set
     * difference does not render unchanged elements.
     *
     * @param items     array or collection to render
     * @param operation operation of each entry, {@code null} means the default {@code update}
     * @param filter    predicate of the elements to render
     * @return entries in output order
     */
    public Stream<DiffEntry> entries(Object items, DiffOperation operation, Predicate<Object> filter) {
        if (FastPath.isArray(items.getClass())) {
            return IntStream.range(0, Array.getLength(items))
                    .mapToObj(index -> Array.get(items, index))
                    .filter(filter)
                    .map(value -> body(operation, null, value));
        }
        if (!(items instanceof Collection<?> collection)) {
            throw new IllegalArgumentException(
                    "Expected an array or a collection, got " + items.getClass().getName());
        }
        List<DiffEntry> entries = new ArrayList<>();
        for (Object value : collection) {
            if (filter.test(value)) {
                entries.add(body(operation, null, value));
            }
        }
        if (!FastPath.isOrdered(collection)) {
            entries.sort(Comparator.naturalOrder());
        }
        return entries.stream();
    }

    private IllegalStateException collision(Map<?, ?> map, boolean skipNulls,
                                            DiffKey rendered, Object secondKey) {
        Object firstKey = null;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (skipNulls && entry.getValue() == null) {
                continue;
            }
            if (renderService.renderKey(entry.getKey(), this).equals(rendered)) {
                firstKey = entry.getKey();
                break;
            }
        }
        throw new IllegalStateException(String.format(
                "Map keys %s and %s are rendered as the same %s. Register a presenter "
                        + "via TestSetupModule.addDiffKeyPresenter that renders them differently",
                describeKey(firstKey), describeKey(secondKey), rendered.description()));
    }

    private String describeKey(Object key) {
        return key == null ? "null" : key.getClass().getSimpleName() + " '" + key + "'";
    }

    /**
     * Semantic type name of a value, written as the {@code type} attribute. It is also the default
     * type of the presenter; a presenter calls it when the element type has to differ from the
     * value type, for example a Redis hash projected as a map.
     *
     * @param value value to inspect, may be {@code null}
     * @return type name, or {@code null} when the value has no explicit type
     */
    public String typeOf(Object value) {
        return valueFormat.typeOf(value);
    }
}

package io.github.dimkich.integration.testing.storage.sql.table;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Dense map-like storage indexed by {@link Table#getId()}. It provides direct lookup by a canonical
 * table without hashing and without boxing on reads.
 * <p>
 * This is intentionally not a general {@link java.util.Map}: it has no key set or entry set.
 * Values are non-null; {@link #remove(Table)} explicitly clears an entry. Values can be visited
 * with {@link #forEach(Consumer)} in table id order; holes are skipped. The order is deterministic
 * but not insertion order.
 */
public final class TableMap<V> {
    private Object[] elements;

    public TableMap() {
        this(0);
    }

    public TableMap(int initialCapacity) {
        elements = new Object[Math.max(0, initialCapacity)];
    }

    @SuppressWarnings("unchecked")
    public V get(Table table) {
        int index = table.getId();
        return index < elements.length ? (V) elements[index] : null;
    }

    /**
     * Returns the value stored for the table, failing when the table was not initialized.
     *
     * @throws IllegalStateException when no value is stored for the table
     */
    public V getRequired(Table table) {
        V value = get(table);
        if (value == null) {
            throw new IllegalStateException("No value for table '" + table + "'");
        }
        return value;
    }

    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public V put(Table table, V value) {
        Objects.requireNonNull(value, "value");
        int index = table.getId();
        if (index >= elements.length) {
            elements = Arrays.copyOf(elements,
                    Math.max(index + 1, Math.max(1, elements.length * 2)));
        }
        V previous = (V) elements[index];
        elements[index] = value;
        return previous;
    }

    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public V remove(Table table) {
        int index = table.getId();
        if (index >= elements.length) {
            return null;
        }
        V previous = (V) elements[index];
        elements[index] = null;
        return previous;
    }

    public V computeIfAbsent(Table table, Function<Table, V> factory) {
        V value = get(table);
        if (value == null) {
            value = factory.apply(table);
            if (value != null) {
                put(table, value);
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super V> action) {
        Objects.requireNonNull(action, "action");
        for (Object element : elements) {
            if (element != null) {
                action.accept((V) element);
            }
        }
    }

    public void clear() {
        Arrays.fill(elements, null);
    }

    public TableMap<V> copy() {
        TableMap<V> copy = new TableMap<>(elements.length);
        System.arraycopy(elements, 0, copy.elements, 0, elements.length);
        return copy;
    }
}

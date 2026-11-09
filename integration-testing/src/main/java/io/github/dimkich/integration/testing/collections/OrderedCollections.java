package io.github.dimkich.integration.testing.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Ordered replacements for JDK collection factories.
 * <p>
 * This class is an implementation detail of {@link DeterministicCollections}: its methods are
 * substituted into the instrumented bytecode instead of {@code Map.of}, {@code Set.of},
 * {@code Collectors.toSet} and other order-sensitive JDK factories.
 * </p>
 * <p>
 * Every method preserves the contract of the replaced JDK API: {@code Map.of}/{@code Set.of}
 * and {@code copyOf}/{@code toUnmodifiable*} stay unmodifiable, {@code toSet}/{@code toMap}/
 * {@code groupingBy} stay mutable, and null/duplicate validation is performed in the same
 * cases as in the JDK. Only the iteration order becomes predictable (insertion order).
 * </p>
 */
public class OrderedCollections {

    private OrderedCollections() {
    }

    /**
     * Ordered replacement for {@link Map#of()}.
     */
    public static <K, V> Map<K, V> mapOf() {
        return Collections.unmodifiableMap(new LinkedHashMap<>());
    }

    /**
     * Ordered replacement for {@link Map#of(Object, Object)}.
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1) {
        return mapOfEntries(Map.entry(k1, v1));
    }

    /**
     * Ordered replacement for {@code Map.of(Object, Object, Object, Object)}.
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
        return mapOfEntries(Map.entry(k1, v1), Map.entry(k2, v2));
    }

    /**
     * Ordered replacement for {@code Map.of(Object, Object, Object, Object, Object, Object)}.
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        return mapOfEntries(Map.entry(k1, v1), Map.entry(k2, v2), Map.entry(k3, v3));
    }

    /**
     * Ordered replacement for the four-entry {@code Map.of} overload.
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return mapOfEntries(Map.entry(k1, v1), Map.entry(k2, v2), Map.entry(k3, v3),
                Map.entry(k4, v4));
    }

    /**
     * Ordered replacement for the five-entry {@code Map.of} overload.
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
                                         K k5, V v5) {
        return mapOfEntries(Map.entry(k1, v1), Map.entry(k2, v2), Map.entry(k3, v3),
                Map.entry(k4, v4), Map.entry(k5, v5));
    }

    /**
     * Ordered replacement for the six-entry {@code Map.of} overload.
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
                                         K k5, V v5, K k6, V v6) {
        return mapOfEntries(Map.entry(k1, v1), Map.entry(k2, v2), Map.entry(k3, v3),
                Map.entry(k4, v4), Map.entry(k5, v5), Map.entry(k6, v6));
    }

    /**
     * Ordered replacement for the seven-entry {@code Map.of} overload.
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
                                         K k5, V v5, K k6, V v6, K k7, V v7) {
        return mapOfEntries(Map.entry(k1, v1), Map.entry(k2, v2), Map.entry(k3, v3),
                Map.entry(k4, v4), Map.entry(k5, v5), Map.entry(k6, v6), Map.entry(k7, v7));
    }

    /**
     * Ordered replacement for the eight-entry {@code Map.of} overload.
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
                                         K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        return mapOfEntries(Map.entry(k1, v1), Map.entry(k2, v2), Map.entry(k3, v3),
                Map.entry(k4, v4), Map.entry(k5, v5), Map.entry(k6, v6), Map.entry(k7, v7),
                Map.entry(k8, v8));
    }

    /**
     * Ordered replacement for the nine-entry {@code Map.of} overload.
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
                                         K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8,
                                         K k9, V v9) {
        return mapOfEntries(Map.entry(k1, v1), Map.entry(k2, v2), Map.entry(k3, v3),
                Map.entry(k4, v4), Map.entry(k5, v5), Map.entry(k6, v6), Map.entry(k7, v7),
                Map.entry(k8, v8), Map.entry(k9, v9));
    }

    /**
     * Ordered replacement for the ten-entry {@code Map.of} overload.
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4,
                                         K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8,
                                         K k9, V v9, K k10, V v10) {
        return mapOfEntries(Map.entry(k1, v1), Map.entry(k2, v2), Map.entry(k3, v3),
                Map.entry(k4, v4), Map.entry(k5, v5), Map.entry(k6, v6), Map.entry(k7, v7),
                Map.entry(k8, v8), Map.entry(k9, v9), Map.entry(k10, v10));
    }

    /**
     * Ordered replacement for {@code Map.ofEntries(...)}.
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mapOfEntries(Map.Entry<? extends K, ? extends V>... entries) {
        Objects.requireNonNull(entries);
        LinkedHashMap<K, V> map = new LinkedHashMap<>(entries.length * 2);
        for (Map.Entry<? extends K, ? extends V> entry : entries) {
            Objects.requireNonNull(entry);
            put(map, entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Ordered replacement for {@link Map#copyOf(Map)}.
     */
    public static <K, V> Map<K, V> mapCopyOf(Map<? extends K, ? extends V> map) {
        Objects.requireNonNull(map);
        LinkedHashMap<K, V> copy = new LinkedHashMap<>(map.size() * 2);
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(copy, entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Ordered replacement for {@link Set#of()}.
     */
    public static <E> Set<E> setOf() {
        return Collections.unmodifiableSet(new LinkedHashSet<>());
    }

    /**
     * Ordered replacement for {@code Set.of(Object)}.
     */
    public static <E> Set<E> setOf(E e1) {
        return createSet(2, e1);
    }

    /**
     * Ordered replacement for {@code Set.of(Object, Object)}.
     */
    public static <E> Set<E> setOf(E e1, E e2) {
        return createSet(4, e1, e2);
    }

    /**
     * Ordered replacement for the three-element {@code Set.of} overload.
     */
    public static <E> Set<E> setOf(E e1, E e2, E e3) {
        return createSet(6, e1, e2, e3);
    }

    /**
     * Ordered replacement for the four-element {@code Set.of} overload.
     */
    public static <E> Set<E> setOf(E e1, E e2, E e3, E e4) {
        return createSet(8, e1, e2, e3, e4);
    }

    /**
     * Ordered replacement for the five-element {@code Set.of} overload.
     */
    public static <E> Set<E> setOf(E e1, E e2, E e3, E e4, E e5) {
        return createSet(10, e1, e2, e3, e4, e5);
    }

    /**
     * Ordered replacement for the six-element {@code Set.of} overload.
     */
    public static <E> Set<E> setOf(E e1, E e2, E e3, E e4, E e5, E e6) {
        return createSet(12, e1, e2, e3, e4, e5, e6);
    }

    /**
     * Ordered replacement for the seven-element {@code Set.of} overload.
     */
    public static <E> Set<E> setOf(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return createSet(14, e1, e2, e3, e4, e5, e6, e7);
    }

    /**
     * Ordered replacement for the eight-element {@code Set.of} overload.
     */
    public static <E> Set<E> setOf(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return createSet(16, e1, e2, e3, e4, e5, e6, e7, e8);
    }

    /**
     * Ordered replacement for the nine-element {@code Set.of} overload.
     */
    public static <E> Set<E> setOf(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return createSet(18, e1, e2, e3, e4, e5, e6, e7, e8, e9);
    }

    /**
     * Ordered replacement for the ten-element {@code Set.of} overload.
     */
    public static <E> Set<E> setOf(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return createSet(20, e1, e2, e3, e4, e5, e6, e7, e8, e9, e10);
    }

    /**
     * Ordered replacement for {@code Set.of(Object[])}.
     */
    @SafeVarargs
    public static <E> Set<E> setOf(E... elements) {
        Objects.requireNonNull(elements);
        return createSet(elements.length * 2, elements);
    }

    /**
     * Ordered replacement for {@link Set#copyOf(Collection)}.
     */
    public static <E> Set<E> setCopyOf(Collection<? extends E> collection) {
        Objects.requireNonNull(collection);
        LinkedHashSet<E> copy = new LinkedHashSet<>(collection.size() * 2);
        for (E element : collection) {
            if (element == null) {
                throw new NullPointerException();
            }
            copy.add(element);
        }
        return Collections.unmodifiableSet(copy);
    }

    /**
     * Ordered replacement for {@link Collectors#toSet()}.
     */
    public static <T> Collector<T, ?, Set<T>> toSet() {
        return Collectors.toCollection(LinkedHashSet::new);
    }

    /**
     * Ordered replacement for {@link Collectors#toUnmodifiableSet()}.
     */
    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        return Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                OrderedCollections::unmodifiableSet);
    }

    /**
     * Ordered replacement for {@link Collectors#toMap(Function, Function)}.
     */
    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper) {
        return Collectors.toMap(keyMapper, valueMapper, throwingMerger(), LinkedHashMap::new);
    }

    /**
     * Ordered replacement for {@link Collectors#toMap(Function, Function, BinaryOperator)}.
     */
    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper,
            BinaryOperator<U> mergeFunction) {
        return Collectors.toMap(keyMapper, valueMapper, mergeFunction, LinkedHashMap::new);
    }

    /**
     * Ordered replacement for {@link Collectors#toUnmodifiableMap(Function, Function)}.
     */
    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper) {
        return Collectors.collectingAndThen(
                OrderedCollections.<T, K, U>toMap(keyMapper, valueMapper),
                OrderedCollections::unmodifiableMap);
    }

    /**
     * Ordered replacement for {@link Collectors#toUnmodifiableMap(Function, Function, BinaryOperator)}.
     */
    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper,
            BinaryOperator<U> mergeFunction) {
        return Collectors.collectingAndThen(
                OrderedCollections.<T, K, U>toMap(keyMapper, valueMapper, mergeFunction),
                OrderedCollections::unmodifiableMap);
    }

    /**
     * Ordered replacement for {@link Collectors#groupingBy(Function)}.
     */
    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(
            Function<? super T, ? extends K> classifier) {
        return Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList());
    }

    /**
     * Ordered replacement for {@link Collectors#groupingBy(Function, Collector)}.
     */
    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(
            Function<? super T, ? extends K> classifier,
            Collector<? super T, A, D> downstream) {
        return Collectors.groupingBy(classifier, LinkedHashMap::new, downstream);
    }

    private static <K, V> void put(Map<K, V> map, K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        if (map.put(key, value) != null) {
            throw new IllegalArgumentException("duplicate key: " + key);
        }
    }

    @SafeVarargs
    private static <E> Set<E> createSet(int capacity, E... elements) {
        LinkedHashSet<E> set = new LinkedHashSet<>(capacity);
        for (E element : elements) {
            add(set, element);
        }
        return Collections.unmodifiableSet(set);
    }

    private static <E> void add(Set<E> set, E element) {
        if (element == null) {
            throw new NullPointerException();
        }
        if (!set.add(element)) {
            throw new IllegalArgumentException("duplicate element: " + element);
        }
    }

    private static <K, V> Map<K, V> unmodifiableMap(Map<K, V> map) {
        if (map.containsKey(null) || map.containsValue(null)) {
            throw new NullPointerException();
        }
        return Collections.unmodifiableMap(map);
    }

    private static <E> Set<E> unmodifiableSet(Set<E> set) {
        if (set.contains(null)) {
            throw new NullPointerException();
        }
        return Collections.unmodifiableSet(set);
    }

    private static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }
}

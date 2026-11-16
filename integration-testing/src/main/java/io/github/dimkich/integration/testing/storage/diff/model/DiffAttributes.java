package io.github.dimkich.integration.testing.storage.diff.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.github.dimkich.integration.testing.util.FastPath;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * Metadata attributes of a difference element: a map from a name to a value with a deterministic
 * output order.
 * <p>
 * The value is an ordinary map, so it is written as a JSON object and can be read as a map. A map
 * that keeps an order of its own — {@link LinkedHashMap} or {@link SortedMap} — sets the output
 * order, the same way an ordered body map does. An unordered map, for example {@link java.util.HashMap}
 * or {@code Map.of} with several entries, is sorted by name. A map of one attribute needs no
 * ordering.
 * <p>
 * Attributes are metadata of a value, not a difference: an element carries its complete attribute
 * set, so an absent attribute means that the value has no such attribute.
 * <p>
 * Attributes are usually built with {@link #builder()}: the builder validates each attribute on
 * addition, creates the map lazily and keeps the order policy of every added map.
 * <p>
 * The natural order is deterministic and is used only for output order. Two equal maps may keep
 * different orders, so equality is checked first; maps that differ are compared in name order. The
 * order is consistent with {@code equals}.
 */
public final class DiffAttributes extends AbstractMap<String, String> implements Comparable<DiffAttributes> {
    private static final Comparator<String> TEXTS =
            Comparator.nullsFirst(Comparator.naturalOrder());

    /**
     * Attributes of an element without metadata.
     */
    public static final DiffAttributes EMPTY = new DiffAttributes(Map.of());

    private final Map<String, String> values;

    private DiffAttributes(Map<String, String> values) {
        this(order(values), false);
    }

    private DiffAttributes(Map<String, String> values, boolean validated) {
        this.values = values;
        if (!validated) {
            XmlNames.validateAttributes(values);
        }
    }

    /**
     * Creates attributes from a map. The map is ordered by the policy of this class; {@code null}
     * and an empty map become {@link #EMPTY}.
     *
     * @param attributes attributes by name, may be {@code null}
     * @return ordered attributes, never {@code null}
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static DiffAttributes of(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return EMPTY;
        }
        if (attributes instanceof DiffAttributes diffAttributes) {
            return diffAttributes;
        }
        return new DiffAttributes(attributes);
    }

    /**
     * @return builder of attributes; the builder creates the underlying map lazily
     */
    public static Builder builder() {
        return new Builder();
    }

    @NonNull
    @Override
    public Set<Entry<String, String>> entrySet() {
        return values.entrySet();
    }

    @Override
    public String get(Object key) {
        return values.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return values.containsKey(key);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Compares two attribute maps by size, then by name and value. Two equal maps may keep
     * different orders, so equality is checked first; maps that differ are compared in name order.
     *
     * @param other attributes to compare
     * @return comparison result
     */
    @Override
    public int compareTo(DiffAttributes other) {
        int order = Integer.compare(values.size(), other.values.size());
        if (order != 0 || values.isEmpty() || values.equals(other.values)) {
            return order;
        }
        if (values.size() == 1) {
            Map.Entry<String, String> first = values.entrySet().iterator().next();
            Map.Entry<String, String> second = other.values.entrySet().iterator().next();
            order = TEXTS.compare(first.getKey(), second.getKey());
            return order != 0 ? order : TEXTS.compare(first.getValue(), second.getValue());
        }
        String[] firstNames = values.keySet().toArray(String[]::new);
        Arrays.sort(firstNames, TEXTS);
        String[] secondNames = other.values.keySet().toArray(String[]::new);
        Arrays.sort(secondNames, TEXTS);
        for (int i = 0; i < firstNames.length; i++) {
            order = TEXTS.compare(firstNames[i], secondNames[i]);
            if (order != 0) {
                return order;
            }
            order = TEXTS.compare(values.get(firstNames[i]), other.values.get(secondNames[i]));
            if (order != 0) {
                return order;
            }
        }
        return 0;
    }

    /**
     * Builds attributes: validates every attribute on addition, keeps the order policy of the
     * source maps and creates the underlying map lazily.
     * <p>
     * An attribute added twice with the same value is kept once; a different value is an error,
     * because a flat map of attributes cannot represent both.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Builder {
        private Map<String, String> values;
        private boolean sort;

        /**
         * Adds one attribute.
         *
         * @param name  attribute name
         * @param value attribute value
         * @return this builder
         */
        public Builder addAttribute(String name, String value) {
            put(name, value);
            return this;
        }

        /**
         * Adds all attributes of a map. An ordered map keeps its order; an unordered map of two or
         * more attributes makes the result sorted by name.
         *
         * @param attributes attributes to add, may be {@code null}
         * @return this builder
         */
        public Builder addAttributes(Map<String, String> attributes) {
            if (attributes == null || attributes.isEmpty()) {
                return this;
            }
            sort |= !isOrdered(attributes) && attributes.size() > 1;
            attributes.forEach(this::put);
            return this;
        }

        /**
         * @return attributes, never {@code null}; {@link #EMPTY} when nothing was added
         */
        public DiffAttributes build() {
            if (values == null) {
                return EMPTY;
            }
            Map<String, String> result = sort ? sorted(values) : new LinkedHashMap<>(values);
            return new DiffAttributes(result, true);
        }

        private void put(String name, String value) {
            XmlNames.validateAttribute(name, value);
            if (values == null) {
                values = new LinkedHashMap<>();
            }
            String previous = values.putIfAbsent(name, value);
            if (previous != null && !previous.equals(value)) {
                throw new IllegalStateException("Attribute '" + name
                        + "' has conflicting values '" + previous + "' and '" + value + "'");
            }
        }

        private static Map<String, String> sorted(Map<String, String> values) {
            Map<String, String> ordered = new LinkedHashMap<>(values.size());
            values.keySet().stream().sorted(TEXTS).forEach(name -> ordered.put(name, values.get(name)));
            return ordered;
        }
    }

    /**
     * @return attributes in output order: an ordered map is kept as is, an unordered map of two or
     *         more attributes is sorted by name
     */
    private static Map<String, String> order(Map<String, String> attributes) {
        int size = attributes.size();
        if (size < 2 || isOrdered(attributes)) {
            return attributes;
        }
        String[] names = attributes.keySet().toArray(String[]::new);
        Arrays.sort(names, TEXTS);
        Map<String, String> ordered = new LinkedHashMap<>(size);
        for (String name : names) {
            ordered.put(name, attributes.get(name));
        }
        return ordered;
    }

    /**
     * @return {@code true} when the map keeps an order of its own
     */
    private static boolean isOrdered(Map<String, String> attributes) {
        return attributes instanceof DiffAttributes || FastPath.isOrdered(attributes);
    }
}

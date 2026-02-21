package io.github.dimkich.integration.testing.format.common.map.converter;

import com.fasterxml.jackson.databind.util.StdConverter;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import io.github.dimkich.integration.testing.format.common.factory.ResettableIterator;
import io.github.dimkich.integration.testing.format.common.map.dto.MapEntry;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;

/**
 * Jackson {@link StdConverter} that materializes a container from a list of {@link MapEntry} DTOs.
 * <p>
 * Depending on the configured {@code targetClass}, conversion can produce:
 * <ul>
 *   <li>a {@link Map} implementation,</li>
 *   <li>a {@link Collection} of {@link Map.Entry} values, or</li>
 *   <li>a {@link ResettableIterator} over {@link Map.Entry} values.</li>
 * </ul>
 *
 * @param <K> the type of keys maintained by produced entries
 * @param <V> the type of mapped values
 */
public class MapFromEntriesConverter<K, V>
        extends StdConverter<List<? extends MapEntry<K, V>>, Object> {

    private final Supplier<Object> containerSupplier;
    private final Class<?> targetClass;
    private final boolean isMap;

    /**
     * Creates a converter for the provided target container type.
     * <p>
     * For map targets, the converter builds either:
     * <ul>
     *   <li>a concrete map instance via no-arg constructor,</li>
     *   <li>a {@link TreeMap} for sorted/navigable map abstractions, or</li>
     *   <li>a {@link LinkedHashMap} for other map abstractions.</li>
     * </ul>
     * For non-map targets, it uses a collection of {@link Map.Entry} values.
     *
     * @param targetClass target container class to materialize during conversion
     */
    @SuppressWarnings("unchecked")
    public MapFromEntriesConverter(Class<?> targetClass) {
        this.targetClass = targetClass;
        this.isMap = Map.class.isAssignableFrom(targetClass);

        if (!isMap) {
            this.containerSupplier = ArrayList::new;
        } else if (targetClass.isInterface() || Modifier.isAbstract(targetClass.getModifiers())) {
            if (NavigableMap.class.isAssignableFrom(targetClass) || SortedMap.class.isAssignableFrom(targetClass)) {
                this.containerSupplier = TreeMap::new;
            } else {
                this.containerSupplier = LinkedHashMap::new;
            }
        } else {
            Class<Map<K, V>> cls = (Class<Map<K, V>>) targetClass;
            this.containerSupplier = SneakySupplier.sneaky(() ->
                    cls.getDeclaredConstructor().newInstance());
        }
    }

    /**
     * Converts DTO entries into the configured target representation.
     * <p>
     * Returns:
     * <ul>
     *   <li>a map when {@code targetClass} is assignable to {@link Map},</li>
     *   <li>a {@link ResettableIterator} of map entries when target is an {@link Iterator},</li>
     *   <li>or a collection of {@link Map.Entry} values otherwise.</li>
     * </ul>
     *
     * @param list source entries to convert; may be {@code null}
     * @return converted container instance, or {@code null} when input is {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object convert(List<? extends MapEntry<K, V>> list) {
        if (list == null) return null;

        Object container = containerSupplier.get();

        if (isMap) {
            Map<K, V> map = (Map<K, V>) container;
            for (MapEntry<K, V> entry : list) {
                map.put(entry.getKey(), entry.getValue());
            }
            return map;
        } else {
            Collection<Map.Entry<K, V>> collection = (Collection<Map.Entry<K, V>>) container;
            for (MapEntry<K, V> entry : list) {
                collection.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            }
            if (Iterator.class.isAssignableFrom(targetClass)) {
                return new ResettableIterator<>(list.stream()
                        .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue()))
                        .toList()
                );
            }
            return collection;
        }
    }
}

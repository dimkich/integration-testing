package io.github.dimkich.integration.testing.format.common.map.converter;

import com.fasterxml.jackson.databind.util.StdConverter;
import io.github.dimkich.integration.testing.format.common.factory.ResettableIterator;
import io.github.dimkich.integration.testing.format.common.map.dto.MapEntry;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Converts map-like values into a list of {@link MapEntry} DTO instances.
 *
 * <p>The converter accepts {@link Map}, {@link Collection} of {@link java.util.Map.Entry},
 * {@link Iterator}, and {@link ResettableIterator} inputs.
 *
 * @param <K> map key type
 * @param <V> map value type
 */
@RequiredArgsConstructor
public class MapToEntriesConverter<K, V> extends StdConverter<Object, List<MapEntry<K, V>>> {
    private final Class<? extends MapEntry<K, V>> mapEntryClass;

    /**
     * Converts the given value to a list of {@link MapEntry} objects.
     *
     * <p>If the source value is a {@link ResettableIterator}, it is reset before and after
     * iteration to keep the iterator reusable.
     *
     * @param value source object to convert
     * @return list of converted entries, or {@code null} when input is {@code null}
     * @throws IllegalArgumentException if the source type is not supported
     */
    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public List<MapEntry<K, V>> convert(Object value) {
        if (value == null) return null;

        List<MapEntry<K, V>> result = new ArrayList<>();
        Iterable<Map.Entry<K, V>> entries;

        if (value instanceof Map) {
            entries = ((Map<K, V>) value).entrySet();
        } else if (value instanceof Collection<?>) {
            entries = (Collection<Map.Entry<K, V>>) value;
        } else if (value instanceof ResettableIterator<?> iterator) {
            iterator.reset();
            entries = () -> (Iterator<Map.Entry<K, V>>) value;
        } else if (value instanceof Iterator<?>) {
            entries = () -> (Iterator<Map.Entry<K, V>>) value;
        } else {
            throw new IllegalArgumentException("Unsupported type for conversion: " + value.getClass());
        }

        Constructor<? extends MapEntry<K, V>> constructor = mapEntryClass.getDeclaredConstructor();
        for (Map.Entry<K, V> entry : entries) {
            MapEntry<K, V> mapEntry = constructor.newInstance();
            mapEntry.setKey(entry.getKey());
            mapEntry.setValue(entry.getValue());
            result.add(mapEntry);
        }
        if (value instanceof ResettableIterator<?> iterator) {
            iterator.reset();
        }
        return result;
    }
}

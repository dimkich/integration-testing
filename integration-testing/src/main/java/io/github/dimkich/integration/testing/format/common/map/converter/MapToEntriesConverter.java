package io.github.dimkich.integration.testing.format.common.map.converter;

import com.fasterxml.jackson.databind.util.StdConverter;
import io.github.dimkich.integration.testing.format.common.map.dto.MapEntry;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Jackson {@link StdConverter} that converts a {@link Map} into a {@link List}
 * of {@link MapEntry} objects.
 * <p>
 * Each original map entry is transformed into a new instance of the provided
 * {@code mapEntryClass}, with its key and value fields populated from the
 * source map.
 *
 * @param <K> type of map keys
 * @param <V> type of map values
 */

@RequiredArgsConstructor
public class MapToEntriesConverter<K, V> extends StdConverter<Map<K, V>, List<MapEntry<K, V>>> {
    private final Class<? extends MapEntry<K, V>> mapEntryClass;

    /**
     * Converts the given {@link Map} into a {@link List} of {@link MapEntry}
     * instances.
     *
     * @param map source map to convert; must not be {@code null}
     * @return list of {@link MapEntry} objects representing the entries of the given map
     */

    @Override
    @SneakyThrows
    public List<MapEntry<K, V>> convert(Map<K, V> map) {
        List<MapEntry<K, V>> result = new ArrayList<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            MapEntry<K, V> mapEntry = mapEntryClass.getDeclaredConstructor().newInstance();
            mapEntry.setKey(entry.getKey());
            mapEntry.setValue(entry.getValue());
            result.add(mapEntry);
        }
        return result;
    }
}

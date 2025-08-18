package io.github.dimkich.integration.testing.format.xml.map;

import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class MapToEntriesConverter<K, V> extends StdConverter<Map<K, V>, List<MapEntry<K, V>>> {
    private final Class<? extends MapEntry> mapEntryClass;

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

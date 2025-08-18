package io.github.dimkich.integration.testing.format.xml.map;

import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class MapFromEntriesConverter<K, V> extends StdConverter<List<MapEntry<K, V>>, Map<K, V>> {
    private final Class<?> mapClass;
    @Override
    @SneakyThrows
    public Map<K, V> convert(List<MapEntry<K, V>> list) {
        Map<K, V> result = mapClass.isInterface() ? new LinkedHashMap<>() : (Map<K, V>) mapClass.getConstructor().newInstance();
        for (MapEntry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}

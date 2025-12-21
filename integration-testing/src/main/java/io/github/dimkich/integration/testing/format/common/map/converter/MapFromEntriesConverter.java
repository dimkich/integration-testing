package io.github.dimkich.integration.testing.format.common.map.converter;

import com.fasterxml.jackson.databind.util.StdConverter;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import io.github.dimkich.integration.testing.format.common.map.dto.MapEntry;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MapFromEntriesConverter<K, V>
        extends StdConverter<List<? extends MapEntry<K, V>>, Map<K, V>> {
    private final Supplier<Map<K, V>> mapSupplier;

    @SuppressWarnings("unchecked")
    public MapFromEntriesConverter(Class<?> mapClass) {
        if (mapClass.isInterface() || Modifier.isAbstract(mapClass.getModifiers())) {
            this.mapSupplier = LinkedHashMap::new;
        } else {
            Class<Map<K, V>> cls = (Class<Map<K, V>>) mapClass;
            this.mapSupplier = SneakySupplier.sneaky(() ->
                    cls.getDeclaredConstructor().newInstance());
        }
    }

    @Override
    public Map<K, V> convert(List<? extends MapEntry<K, V>> list) {
        Map<K, V> result = mapSupplier.get();
        for (MapEntry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}

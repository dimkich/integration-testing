package io.github.dimkich.integration.testing.format.common.map.converter;

import com.fasterxml.jackson.databind.util.StdConverter;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import io.github.dimkich.integration.testing.format.common.map.dto.MapEntry;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Jackson {@link StdConverter} that builds a {@link Map} instance
 * from a list of {@link MapEntry} DTOs.
 * <p>
 * When the provided {@code mapClass} is an interface or an abstract class, a {@link LinkedHashMap}
 * is used to preserve insertion order. Otherwise, the converter attempts to instantiate the concrete
 * {@code mapClass} via its no-argument constructor.
 *
 * @param <K> the type of keys maintained by the resulting map
 * @param <V> the type of mapped values
 */
public class MapFromEntriesConverter<K, V>
        extends StdConverter<List<? extends MapEntry<K, V>>, Map<K, V>> {
    private final Supplier<Map<K, V>> mapSupplier;

    /**
     * Creates a new converter for the given target map type.
     * <p>
     * If {@code mapClass} is an interface or an abstract class, a {@link LinkedHashMap}
     * will be used as the concrete implementation.
     *
     * @param mapClass concrete map implementation class or a map interface/abstract
     *                 type; if it is not instantiable, {@link LinkedHashMap} will be used
     */
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

    /**
     * Converts a list of {@link MapEntry} DTOs into a {@link Map} instance
     * produced by this converter's {@link #mapSupplier}.
     *
     * @param list list of map entries to be converted; expected to be non-null
     * @return a new map populated with all provided entries
     */
    @Override
    public Map<K, V> convert(List<? extends MapEntry<K, V>> list) {
        Map<K, V> result = mapSupplier.get();
        for (MapEntry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}

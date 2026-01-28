package io.github.dimkich.integration.testing.format.common.map.converter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdConverter;
import io.github.dimkich.integration.testing.format.common.map.dto.MapEntry;
import io.github.dimkich.integration.testing.format.common.map.dto.WrappedMap;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Jackson {@link StdConverter} that converts a {@link Map} into a {@link WrappedMap}
 * containing {@link MapEntry} DTOs.
 * <p>
 * The actual transformation of the {@link Map} into a list of {@link MapEntry}
 * instances is delegated to the underlying {@link MapToEntriesConverter}.
 *
 * @param <K> type of map keys
 * @param <V> type of map values
 */
@RequiredArgsConstructor
public class WrappedMapToEntriesConverter<K, V> extends StdConverter<Map<K, V>,
        WrappedMap<MapEntry<K, V>, K, V>> {
    private final MapToEntriesConverter<K, V> mapToEntriesConverter;

    /**
     * Wraps the given {@link Map} into a {@link WrappedMap} whose entries are
     * produced by the configured {@link #mapToEntriesConverter}.
     *
     * @param map source map to convert; expected to be non-null
     * @return a {@link WrappedMap} containing {@link MapEntry} DTOs created from the given map
     */
    @Override
    public WrappedMap<MapEntry<K, V>, K, V> convert(Map<K, V> map) {
        return new WrappedMap<>(mapToEntriesConverter.convert(map));
    }

    /**
     * Delegates to {@link StdConverter#getInputType(TypeFactory)} to determine
     * the input type of this converter.
     *
     * @param typeFactory Jackson {@link TypeFactory} used to construct type information
     * @return the input type of this converter
     */
    @Override
    public JavaType getInputType(TypeFactory typeFactory) {
        return super.getInputType(typeFactory);
    }

    /**
     * Delegates to {@link StdConverter#getOutputType(TypeFactory)} to determine
     * the output type of this converter.
     *
     * @param typeFactory Jackson {@link TypeFactory} used to construct type information
     * @return the output type of this converter
     */
    @Override
    public JavaType getOutputType(TypeFactory typeFactory) {
        return super.getOutputType(typeFactory);
    }
}

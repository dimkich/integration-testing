package io.github.dimkich.integration.testing.format.common.map.converter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.StdConverter;
import io.github.dimkich.integration.testing.format.common.map.dto.MapEntry;
import io.github.dimkich.integration.testing.format.common.map.dto.WrappedMap;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Jackson {@link StdConverter} that converts a {@link WrappedMap} containing
 * {@link MapEntry} DTOs into a regular {@link Map}.
 * <p>
 * The actual map construction is delegated to the provided
 * {@link Converter} implementation, which is expected to know how to
 * build a concrete {@link Map} from a list of {@link MapEntry} objects.
 *
 * @param <K> type of map keys
 * @param <V> type of map values
 */
@RequiredArgsConstructor
public class WrappedMapFromEntriesConverter<K, V> extends
        StdConverter<WrappedMap<MapEntry<K, V>, K, V>, Map<K, V>> {
    private final Converter<List<? extends MapEntry<K, V>>, Map<K, V>> mapFromEntriesConverter;
    private final JavaType inputType;

    /**
     * Converts the provided {@link WrappedMap} into a {@link Map} instance
     * by delegating to the underlying {@link #mapFromEntriesConverter}.
     *
     * @param value wrapper object holding the list of {@link MapEntry} DTOs;
     *              expected to be non-null
     * @return a map populated from the entries contained in the given wrapper
     */
    @Override
    public Map<K, V> convert(WrappedMap<MapEntry<K, V>, K, V> value) {
        return mapFromEntriesConverter.convert(value.getEntry());
    }

    /**
     * Returns the {@link JavaType} that describes the input type
     * for this converter.
     *
     * @param typeFactory Jackson {@link TypeFactory} (ignored in this implementation)
     * @return the input type that this converter expects
     */
    @Override
    public JavaType getInputType(TypeFactory typeFactory) {
        return inputType;
    }

    /**
     * Delegates to {@link StdConverter#getOutputType(TypeFactory)} to
     * determine the output type of this converter.
     *
     * @param typeFactory Jackson {@link TypeFactory} used to construct type information
     * @return the output type of this converter
     */
    @Override
    public JavaType getOutputType(TypeFactory typeFactory) {
        return super.getOutputType(typeFactory);
    }
}

package io.github.dimkich.integration.testing.format.common.map.converter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.StdConverter;
import io.github.dimkich.integration.testing.format.common.map.dto.MapEntry;
import io.github.dimkich.integration.testing.format.common.map.dto.WrappedMap;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Jackson {@link StdConverter} that unwraps {@link WrappedMap} values containing
 * {@link MapEntry} DTOs and delegates conversion to another converter.
 * <p>
 * The resulting object type is determined by the configured delegate
 * {@link Converter} implementation.
 *
 * @param <K> type of map keys
 * @param <V> type of map values
 */
@RequiredArgsConstructor
public class WrappedMapFromEntriesConverter<K, V> extends
        StdConverter<WrappedMap<MapEntry<K, V>, K, V>, Object> {
    private final Converter<List<? extends MapEntry<K, V>>, Object> mapFromEntriesConverter;
    private final JavaType inputType;

    /**
     * Converts the provided {@link WrappedMap} by extracting its entry list
     * and delegating to the underlying {@link #mapFromEntriesConverter}.
     *
     * @param value wrapper object holding the list of {@link MapEntry} DTOs
     * @return converted container object produced by the delegate converter
     * @throws NullPointerException when {@code value} is {@code null}
     */
    @Override
    public Object convert(WrappedMap<MapEntry<K, V>, K, V> value) {
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

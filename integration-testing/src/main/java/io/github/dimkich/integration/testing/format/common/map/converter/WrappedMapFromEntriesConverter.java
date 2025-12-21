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

@RequiredArgsConstructor
public class WrappedMapFromEntriesConverter<K, V> extends
        StdConverter<WrappedMap<MapEntry<K, V>, K, V>, Map<K, V>> {
    private final Converter<List<? extends MapEntry<K, V>>, Map<K, V>> mapFromEntriesConverter;
    private final JavaType inputType;

    @Override
    public Map<K, V> convert(WrappedMap<MapEntry<K, V>, K, V> value) {
        return mapFromEntriesConverter.convert(value.getEntry());
    }

    @Override
    public JavaType getInputType(TypeFactory typeFactory) {
        return inputType;
    }

    @Override
    public JavaType getOutputType(TypeFactory typeFactory) {
        return super.getOutputType(typeFactory);
    }
}

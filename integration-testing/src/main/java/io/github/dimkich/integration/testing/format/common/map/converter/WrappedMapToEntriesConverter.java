package io.github.dimkich.integration.testing.format.common.map.converter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdConverter;
import io.github.dimkich.integration.testing.format.common.map.dto.MapEntry;
import io.github.dimkich.integration.testing.format.common.map.dto.WrappedMap;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class WrappedMapToEntriesConverter<K, V> extends StdConverter<Map<K, V>,
        WrappedMap<MapEntry<K, V>, K, V>> {
    private final MapToEntriesConverter<K, V> mapToEntriesConverter;

    @Override
    public WrappedMap<MapEntry<K, V>, K, V> convert(Map<K, V> map) {
        return new WrappedMap<>(mapToEntriesConverter.convert(map));
    }

    @Override
    public JavaType getInputType(TypeFactory typeFactory) {
        return super.getInputType(typeFactory);
    }

    @Override
    public JavaType getOutputType(TypeFactory typeFactory) {
        return super.getOutputType(typeFactory);
    }
}

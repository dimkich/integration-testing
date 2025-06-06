package io.github.dimkich.integration.testing.xml.map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class WrappedMapFromEntriesConverter<E extends MapEntry<K, V>, K, V> extends StdConverter<WrappedMap<E, K, V>, Map<K, V>> {
    private final MapFromEntriesConverter<K, V> mapFromEntriesConverter;
    private final JavaType inputType;

    @Override
    public Map<K, V> convert(WrappedMap<E, K, V> value) {
        return mapFromEntriesConverter.convert((List<MapEntry<K,V>>) value.getEntry());
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

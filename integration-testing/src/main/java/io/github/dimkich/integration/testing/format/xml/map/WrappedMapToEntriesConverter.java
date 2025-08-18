package io.github.dimkich.integration.testing.format.xml.map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class WrappedMapToEntriesConverter<E extends MapEntry<K, V>, K, V> extends StdConverter<Map<K, V>, WrappedMap<E, K, V>> {
    private final MapToEntriesConverter<K, V> mapToEntriesConverter;

    @Override
    public WrappedMap<E, K, V> convert(Map<K, V> map) {
        return new WrappedMap<>((List<E>) mapToEntriesConverter.convert(map));
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

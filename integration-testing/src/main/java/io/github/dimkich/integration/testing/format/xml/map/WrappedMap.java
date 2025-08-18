package io.github.dimkich.integration.testing.format.xml.map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WrappedMap<E extends MapEntry<K, V>, K, V> {
    private List<E> entry;
}

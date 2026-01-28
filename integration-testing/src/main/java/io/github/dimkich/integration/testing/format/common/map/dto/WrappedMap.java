package io.github.dimkich.integration.testing.format.common.map.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper DTO that represents a collection of map entries.
 * <p>
 * Useful for formats (for example XML) where a map is serialized
 * as an enclosing object that contains a list of entry elements.
 *
 * @param <E> concrete entry type that extends {@link MapEntry}
 * @param <K> key type of the wrapped map
 * @param <V> value type of the wrapped map
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WrappedMap<E extends MapEntry<K, V>, K, V> {

    /**
     * Collection of wrapped map entries.
     */
    private List<E> entry;
}

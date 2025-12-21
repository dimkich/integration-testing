package io.github.dimkich.integration.testing.format.common.map.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({"key", "value"})
public class MapEntryKeyAsElement<K, V> implements MapEntry<K, V> {
    private K key;
    private V value;
}

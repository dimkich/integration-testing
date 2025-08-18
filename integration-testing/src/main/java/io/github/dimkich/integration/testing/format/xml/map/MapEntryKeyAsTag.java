package io.github.dimkich.integration.testing.format.xml.map;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonPropertyOrder({"key", "value"})
public class MapEntryKeyAsTag<K, V> implements MapEntry<K, V> {
    private K key;
    private V value;
}

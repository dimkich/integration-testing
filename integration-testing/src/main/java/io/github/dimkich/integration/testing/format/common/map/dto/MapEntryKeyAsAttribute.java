package io.github.dimkich.integration.testing.format.common.map.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonPropertyOrder({"key", "value"})
public class MapEntryKeyAsAttribute<K, V> implements MapEntry<K, V> {
    @JacksonXmlProperty(isAttribute = true)
    private K key;
    @JsonUnwrapped
    private V value;
}

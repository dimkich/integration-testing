package io.github.dimkich.integration.testing.format.xml.map;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@JsonPropertyOrder({"key", "value"})
public class MapEntryKeyAsAttribute<K, V> implements MapEntry<K, V> {
    @JacksonXmlProperty(isAttribute = true)
    private K key;
    @JsonUnwrapped
    @Setter
    private V value;
}

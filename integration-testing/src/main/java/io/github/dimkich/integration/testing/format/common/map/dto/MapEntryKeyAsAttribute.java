package io.github.dimkich.integration.testing.format.common.map.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * Map entry representation where the key is serialized as an XML attribute
 * and the value is serialized as the element body (unwrapped).
 *
 * @param <K> type of the entry key
 * @param <V> type of the entry value
 */
@Data
@JsonPropertyOrder({"key", "value"})
public class MapEntryKeyAsAttribute<K, V> implements MapEntry<K, V> {
    /**
     * Map entry key serialized as an XML attribute.
     */
    @JacksonXmlProperty(isAttribute = true)
    private K key;

    /**
     * Map entry value serialized as the element body (unwrapped).
     */
    @JsonUnwrapped
    private V value;
}

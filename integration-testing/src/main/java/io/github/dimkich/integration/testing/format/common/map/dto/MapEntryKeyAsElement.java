package io.github.dimkich.integration.testing.format.common.map.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

/**
 * Map entry representation where the key is serialized as an XML element
 * and the value is serialized as a nested element or field.
 *
 * @param <K> type of the entry key
 * @param <V> type of the entry value
 */
@Data
@JsonPropertyOrder({"key", "value"})
public class MapEntryKeyAsElement<K, V> implements MapEntry<K, V> {
    /**
     * Map entry key serialized as an XML element.
     */
    private K key;

    /**
     * Map entry value serialized as a nested element or field.
     */
    private V value;
}

package io.github.dimkich.integration.testing.format.common.map.dto;

/**
 * Represents a key-value pair entry used for map serialization and deserialization.
 * <p>
 * This interface is used as part of the map serialization mechanism, allowing maps
 * to be serialized in different formats (e.g., with keys as XML attributes or elements).
 * Implementations of this interface control how map entries are formatted during
 * JSON/XML serialization.
 * <p>
 * This interface is typically used in conjunction with {@code @JsonMapAsEntries} annotation
 * to customize how maps are serialized. Different implementations handle different
 * entry formats:
 * <ul>
 *   <li>{@code MapEntryKeyAsAttribute} - Keys are serialized as XML attributes</li>
 *   <li>{@code MapEntryKeyAsElement} - Keys are serialized as XML elements</li>
 * </ul>
 *
 * @param <K> the type of keys maintained by this map entry
 * @param <V> the type of mapped values
 * @see io.github.dimkich.integration.testing.format.common.map.JsonMapAsEntries
 * @see MapEntryKeyAsAttribute
 * @see MapEntryKeyAsElement
 */
public interface MapEntry<K, V> {
    /**
     * Returns the key corresponding to this entry.
     *
     * @return the key corresponding to this entry
     */
    K getKey();

    /**
     * Sets the key for this entry.
     *
     * @param key the key to be set for this entry
     */
    void setKey(K key);

    /**
     * Returns the value corresponding to this entry.
     *
     * @return the value corresponding to this entry
     */
    V getValue();

    /**
     * Sets the value for this entry.
     *
     * @param value the value to be set for this entry
     */
    void setValue(V value);
}

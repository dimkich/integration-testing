package io.github.dimkich.integration.testing.format.common.map;

import java.util.LinkedHashMap;

/**
 * A specialized {@link LinkedHashMap} implementation configured for XML/JSON serialization
 * where map entries are serialized with keys as XML attributes and entries wrapped in a container.
 * <p>
 * This class extends {@link LinkedHashMap} to maintain insertion order while providing
 * custom serialization behavior through the {@link JsonMapAsEntries} annotation.
 * <p>
 * <b>Serialization Format:</b>
 * <p>
 * Map entries are serialized with the following structure:
 * <pre>{@code
 * <bids>
 *     <entry key="1.2">12.3</entry>
 *     <entry key="1.33">16.3</entry>
 * </bids>
 * }</pre>
 * <p>
 * Key characteristics:
 * <ul>
 *   <li>Keys are serialized as XML attributes (not elements)</li>
 *   <li>Entries are wrapped in a container element named "entry"</li>
 *   <li>Maintains insertion order (inherited from {@link LinkedHashMap})</li>
 *   <li>Supports generic key and value types, with keys typically being String or simple types</li>
 * </ul>
 * <p>
 * This class is particularly useful when you need to serialize maps with simple keys (like strings
 * or numbers) as attributes, providing a more compact XML representation compared to element-based keys.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see JsonMapAsEntries
 * @see LinkedHashMap
 * @since 0.4.0
 */
@JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ATTRIBUTE, entriesWrapped = true)
public class LinkedHashMapStringObject<K, V> extends LinkedHashMap<K, V> {
}

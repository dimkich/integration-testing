package io.github.dimkich.integration.testing.format.common.map;

import java.util.LinkedHashMap;

/**
 * A specialized {@link LinkedHashMap} implementation configured for XML/JSON serialization
 * where map entries are serialized with keys as XML elements and entries wrapped in a container.
 * <p>
 * This class extends {@link LinkedHashMap} to maintain insertion order while providing
 * custom serialization behavior through the {@link JsonMapAsEntries} annotation.
 * <p>
 * <b>Serialization Format:</b>
 * <p>
 * Map entries are serialized with the following structure:
 * <pre>{@code
 * <entry>
 *     <key type="typeTest">
 *         <id>1</id>
 *         <name>1</name>
 *     </key>
 *     <value type="value" attr="a">
 *         <value type="long">12</value>
 *     </value>
 * </entry>
 * }</pre>
 * <p>
 * Key characteristics:
 * <ul>
 *   <li>Keys are serialized as child XML elements (not attributes)</li>
 *   <li>Entries are wrapped in a container element named "entry"</li>
 *   <li>Maintains insertion order (inherited from {@link LinkedHashMap})</li>
 *   <li>Supports generic key and value types</li>
 * </ul>
 * <p>
 * This class is particularly useful when you need to serialize complex objects as map keys
 * or values, as it allows the full structure of both keys and values to be represented in XML.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see JsonMapAsEntries
 * @see LinkedHashMap
 * @since 0.4.0
 */
@JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ELEMENT, entriesWrapped = true)
public class LinkedHashMapObjectObject<K, V> extends LinkedHashMap<K, V> {
}

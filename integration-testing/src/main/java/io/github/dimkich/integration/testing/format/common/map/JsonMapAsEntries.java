package io.github.dimkich.integration.testing.format.common.map;

import io.github.dimkich.integration.testing.format.common.map.dto.MapEntry;
import io.github.dimkich.integration.testing.format.common.map.dto.MapEntryKeyAsAttribute;
import io.github.dimkich.integration.testing.format.common.map.dto.MapEntryKeyAsElement;
import lombok.Getter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to configure how map entries are serialized/deserialized.
 * This annotation can be applied to map types or map fields to control the format
 * of map entries in XML/JSON output.
 * <p>
 * The annotation supports two entry formats:
 * <ul>
 *   <li>{@link EntryFormat#KEY_AS_ATTRIBUTE} - Map keys are serialized as XML attributes</li>
 *   <li>{@link EntryFormat#KEY_AS_ELEMENT} - Map keys are serialized as XML elements</li>
 * </ul>
 * <p>
 * Additionally, entries can be wrapped in a container element when {@code entriesWrapped} is true.
 * <p>
 * <p>
 * <b>Example 1: KEY_AS_ATTRIBUTE with entriesWrapped=false (default)</b>
 * <pre>{@code
 * @JsonMapAsEntries(entryFormat = EntryFormat.KEY_AS_ATTRIBUTE)
 * private TreeMap<BigDecimal, BigDecimal> bids;
 *
 * // Output:
 * // <bid key="1.2">12.3</bid>
 * // <bid key="1.33">16.3</bid>
 * }</pre>
 * <p>
 * <b>Example 2: KEY_AS_ATTRIBUTE with entriesWrapped=true</b>
 * <pre>{@code
 * @JsonMapAsEntries(entryFormat = EntryFormat.KEY_AS_ATTRIBUTE, entriesWrapped = true)
 * private TreeMap<BigDecimal, BigDecimal> bids;
 *
 * // Output:
 * // <bids>
 * //     <entry key="1.2">12.3</entry>
 * //     <entry key="1.33">16.3</entry>
 * // </bids>
 * }</pre>
 * <p>
 * <b>Example 3: KEY_AS_ELEMENT with entriesWrapped=false (default)</b>
 * <pre>{@code
 * @JsonMapAsEntries(entryFormat = EntryFormat.KEY_AS_ELEMENT)
 * public class MapElemNotWrapped<K, V> extends LinkedHashMap<K, V> {}
 *
 * // Output:
 * // <data>
 * //     <key>k1</key>
 * //     <value type="integer">1</value>
 * // </data>
 * // <data>
 * //     <key>k2</key>
 * //     <value>s</value>
 * // </data>
 * }</pre>
 * <p>
 * <b>Example 4: KEY_AS_ELEMENT with entriesWrapped=true</b>
 * <pre>{@code
 * @JsonMapAsEntries(entryFormat = EntryFormat.KEY_AS_ELEMENT, entriesWrapped = true)
 * public class LinkedHashMapObjectObject<K, V> extends LinkedHashMap<K, V> {}
 *
 * // Output:
 * // <entry>
 * //     <key type="typeTest">
 * //         <id>1</id>
 * //         <name>1</name>
 * //     </key>
 * //     <value type="value" attr="a">
 * //         <value type="long">12</value>
 * //     </value>
 * // </entry>
 * }</pre>
 * <p>
 * <b>Example 5: KEY_AS_ATTRIBUTE on field with entriesWrapped=false</b>
 * <pre>{@code
 * @JsonMapAsEntries(entryFormat = EntryFormat.KEY_AS_ATTRIBUTE)
 * private MapAttrNotWrapped<String, Object> data;
 *
 * // Output:
 * // <data key="k1" utype="integer">1</data>
 * // <data key="k2" utype="string">s</data>
 * }</pre>
 *
 * @see EntryFormat
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonMapAsEntries {

    /**
     * Specifies the format for serializing map entries.
     * <p>
     * <b>KEY_AS_ATTRIBUTE:</b> Map keys are serialized as XML attributes on the entry element.
     * <pre>{@code
     * // Example: <entry key="k1">value1</entry>
     * }</pre>
     * <p>
     * <b>KEY_AS_ELEMENT:</b> Map keys are serialized as child XML elements.
     * <pre>{@code
     * // Example: <entry><key>k1</key><value>value1</value></entry>
     * }</pre>
     *
     * @return the entry format to use
     */
    EntryFormat entryFormat();

    /**
     * When true, map entries are wrapped in a container element (typically named "entry").
     * When false (default), entries are serialized directly without a wrapper.
     * <p>
     * <b>entriesWrapped=false:</b>
     * <pre>{@code
     * // <bid key="1.2">12.3</bid>
     * // <bid key="1.33">16.3</bid>
     * }</pre>
     * <p>
     * <b>entriesWrapped=true:</b>
     * <pre>{@code
     * // <bids>
     * //     <entry key="1.2">12.3</entry>
     * //     <entry key="1.33">16.3</entry>
     * // </bids>
     * }</pre>
     *
     * @return true if entries should be wrapped, false otherwise (default: false)
     */
    boolean entriesWrapped() default false;

    /**
     * Enumeration of supported entry formats for map serialization.
     */
    @Getter
    enum EntryFormat {
        /**
         * Map keys are serialized as XML attributes on the entry element.
         * <p>
         * Example output:
         * <pre>{@code
         * <entry key="k1" utype="integer">1</entry>
         * <entry key="k2" utype="string">s</entry>
         * }</pre>
         * <p>
         * Used in examples:
         * <ul>
         *   <li>{@code OrderBook} - {@code @JsonMapAsEntries(entryFormat = KEY_AS_ATTRIBUTE)}</li>
         *   <li>{@code OrderBookWrapped} - with {@code entriesWrapped = true}</li>
         *   <li>{@code MapAttrNotWrapped} - class-level annotation</li>
         *   <li>{@code LinkedHashMapStringObject} - with {@code entriesWrapped = true}</li>
         * </ul>
         */
        KEY_AS_ATTRIBUTE(MapEntryKeyAsAttribute.class),

        /**
         * Map keys are serialized as child XML elements within the entry.
         * <p>
         * Example output:
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
         * Used in examples:
         * <ul>
         *   <li>{@code MapElemNotWrapped} - class-level annotation</li>
         *   <li>{@code LinkedHashMapObjectObject} - with {@code entriesWrapped = true}</li>
         * </ul>
         */
        KEY_AS_ELEMENT(MapEntryKeyAsElement.class);

        private final Class<? extends MapEntry<Object, Object>> cls;

        @SuppressWarnings("unchecked")
        EntryFormat(Class<?> cls) {
            this.cls = (Class<? extends MapEntry<Object, Object>>) cls;
        }
    }
}

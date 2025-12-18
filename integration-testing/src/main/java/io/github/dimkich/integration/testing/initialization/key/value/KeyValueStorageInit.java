package io.github.dimkich.integration.testing.initialization.key.value;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.initialization.TestInit;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * Represents a key-value storage initialization configuration for integration tests.
 * This class extends {@link io.github.dimkich.integration.testing.initialization.TestInit}
 * and provides a mechanism to initialize key-value storage systems (such as Redis, in-memory caches, etc.)
 * by clearing existing data and/or loading initial key-value pairs.
 *
 * <p>Key-value storage initialization is useful when you need to set up test data in
 * key-value stores before running tests. The storage can be cleared and populated
 * with initial data as part of the test initialization phase.
 *
 * <p>Example XML configurations:
 * <pre>{@code
 * <!-- Clear storage and load initial data -->
 * <init type="keyValueStorageInit" name="redisCache" clear="true">
 *     <map>
 *         <key1 type="string">value1</key1>
 *         <key2 type="integer">42</key2>
 *         <key3 type="boolean">true</key3>
 *     </map>
 * </init>
 *
 * <!-- Only load data without clearing -->
 * <init type="keyValueStorageInit" name="redisCache">
 *     <map>
 *         <user:123 type="string">John Doe</user:123>
 *         <session:abc type="string">active</session:abc>
 *     </map>
 * </init>
 *
 * <!-- Only clear storage without loading data -->
 * <init type="keyValueStorageInit" name="redisCache" clear="true"/>
 *
 * <!-- Apply to specific test type -->
 * <init type="keyValueStorageInit" name="redisCache" applyTo="TestCase" clear="true">
 *     <map>
 *         <config:timeout type="integer">5000</config:timeout>
 *     </map>
 * </init>
 * }</pre>
 *
 * @author dimkich
 * @see io.github.dimkich.integration.testing.initialization.TestInit
 * @see io.github.dimkich.integration.testing.storage.keyvalue.KeyValueDataStorage
 */
@Getter
@Setter
@ToString
public class KeyValueStorageInit extends TestInit {
    /**
     * The name of the key-value storage to initialize.
     * This should match the name of a configured {@link io.github.dimkich.integration.testing.storage.keyvalue.KeyValueDataStorage}
     * in the application context.
     * <p>
     * This is serialized as an XML attribute when using Jackson XML serialization.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="keyValueStorageInit" name="redisCache"/>
     * }</pre>
     */
    @JacksonXmlProperty(isAttribute = true)
    private String name;

    /**
     * Whether to clear all existing data from the storage before loading new data.
     * If set to {@code true}, all existing key-value pairs in the storage will be removed
     * before the data from {@code map} is loaded. If set to {@code false} or {@code null},
     * existing data will be preserved and new data will be merged with existing entries.
     * <p>
     * This is serialized as an XML attribute when using Jackson XML serialization.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="keyValueStorageInit" name="redisCache" clear="true"/>
     * }</pre>
     */
    @JacksonXmlProperty(isAttribute = true)
    private Boolean clear;

    /**
     * Map of key-value pairs to load into the storage during initialization.
     * The keys are strings and values can be any object type (strings, numbers, booleans, etc.).
     * <p>
     * In XML, the map is serialized as child elements where each map entry becomes an element
     * with the key as the element name and the value as the element content. The value type
     * is specified using the {@code type} attribute.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="keyValueStorageInit" name="redisCache">
     *     <map>
     *         <key1 type="string">value1</key1>
     *         <key2 type="integer">42</key2>
     *         <key3 type="boolean">true</key3>
     *         <nested:key type="string">nested value</nested:key>
     *     </map>
     * </init>
     * }</pre>
     */
    private Map<String, Object> map;
}

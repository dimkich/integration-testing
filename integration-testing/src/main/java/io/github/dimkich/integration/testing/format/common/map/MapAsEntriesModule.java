package io.github.dimkich.integration.testing.format.common.map;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * A Jackson module that provides custom serialization and deserialization for maps
 * using the {@link JsonMapAsEntries} annotation.
 * <p>
 * This module configures Jackson to serialize and deserialize maps as lists of entries
 * instead of the standard map format. This is particularly useful for XML serialization
 * where map entries can be represented more naturally as a sequence of entry elements.
 * <p>
 * The module registers two modifiers:
 * <ul>
 *   <li>{@link MapAsEntriesSerializerModifier} - Customizes map serialization to convert
 *       maps into lists of {@link io.github.dimkich.integration.testing.format.common.map.dto.MapEntry}
 *       objects or wrapped entries based on the annotation configuration.</li>
 *   <li>{@link MapAsEntriesDeserializerModifier} - Customizes map deserialization to convert
 *       lists of entries back into map instances.</li>
 * </ul>
 * <p>
 * Maps annotated with {@link JsonMapAsEntries} will be serialized/deserialized using
 * the entry-based format, allowing for flexible representation in JSON and XML.
 * <p>
 * This module is typically registered via {@link io.github.dimkich.integration.testing.TestSetupModule#addJacksonModule}
 * and is used in the common format configuration.
 *
 * @see JsonMapAsEntries
 * @see MapAsEntriesSerializerModifier
 * @see MapAsEntriesDeserializerModifier
 * @see com.fasterxml.jackson.databind.module.SimpleModule
 */
public class MapAsEntriesModule extends SimpleModule {
    /**
     * Creates a new MapModule and initializes it with the serializer and deserializer modifiers.
     */
    public MapAsEntriesModule() {
        super();
        setSerializerModifier(new MapAsEntriesSerializerModifier());
        setDeserializerModifier(new MapAsEntriesDeserializerModifier());
    }
}

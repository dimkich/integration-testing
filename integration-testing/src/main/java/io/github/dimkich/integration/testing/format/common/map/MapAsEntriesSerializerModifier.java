package io.github.dimkich.integration.testing.format.common.map;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.CollectionSerializer;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import io.github.dimkich.integration.testing.format.common.factory.TypedStdDelegatingSerializer;
import io.github.dimkich.integration.testing.format.common.map.converter.MapToEntriesConverter;
import io.github.dimkich.integration.testing.format.common.map.converter.WrappedMapToEntriesConverter;
import io.github.dimkich.integration.testing.format.common.map.dto.MapEntry;
import io.github.dimkich.integration.testing.format.common.map.dto.WrappedMap;

import java.util.List;

/**
 * A Jackson {@link BeanSerializerModifier} that customizes map serialization
 * based on the {@link JsonMapAsEntries} annotation.
 * <p>
 * This modifier intercepts map serialization to convert maps into a list of entries
 * or wrapped entries, allowing for custom XML/JSON formatting of map data.
 * <p>
 * The modifier handles two scenarios:
 * <ul>
 *   <li><b>Field-level annotation:</b> When {@link JsonMapAsEntries} is applied to a map field,
 *       the {@link #changeProperties(SerializationConfig, BeanDescription, List)} method
 *       assigns a custom serializer to that property.</li>
 *   <li><b>Type-level annotation:</b> When {@link JsonMapAsEntries} is applied to a map class,
 *       the {@link #modifyMapSerializer(SerializationConfig, MapType, BeanDescription, JsonSerializer)}
 *       method returns a custom serializer for that map type.</li>
 * </ul>
 * <p>
 * The serialization behavior depends on the annotation configuration:
 * <ul>
 *   <li>If {@code entriesWrapped} is {@code false}, maps are serialized as a list of entries
 *       (e.g., {@code [{"key": "k1", "value": "v1"}, ...]})</li>
 *   <li>If {@code entriesWrapped} is {@code true}, maps are serialized as a wrapped container
 *       containing a list of entries (e.g., {@code {"entries": [{"key": "k1", "value": "v1"}, ...]}})</li>
 * </ul>
 *
 * @see JsonMapAsEntries
 * @see BeanSerializerModifier
 */
public class MapAsEntriesSerializerModifier extends BeanSerializerModifier {
    /**
     * Modifies bean properties to apply custom serialization for map fields
     * annotated with {@link JsonMapAsEntries}.
     * <p>
     * This method iterates through all bean properties and checks for the
     * {@link JsonMapAsEntries} annotation. If found on a map-typed property,
     * it assigns a custom serializer that converts the map to entries format.
     *
     * @param config         the serialization configuration
     * @param beanDesc       the bean description containing metadata about the bean class
     * @param beanProperties the list of bean property writers to potentially modify
     * @return the (possibly modified) list of bean property writers
     */
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
        for (BeanPropertyWriter property : beanProperties) {
            JsonMapAsEntries jsonMapAsEntries = property.getAnnotation(JsonMapAsEntries.class);
            if (jsonMapAsEntries != null) {
                property.assignSerializer(createMapSerializer(config.getTypeFactory(), jsonMapAsEntries, property.getType()));
            }
        }
        return beanProperties;
    }

    /**
     * Modifies the map serializer for map types annotated with {@link JsonMapAsEntries}
     * at the class level.
     * <p>
     * This method checks if the map type's class has the {@link JsonMapAsEntries} annotation.
     * If present, it returns a custom serializer that converts the map to entries format.
     * Otherwise, it delegates to the default serializer.
     *
     * @param config     the serialization configuration
     * @param valueType  the map type being serialized
     * @param beanDesc   the bean description containing class annotations
     * @param serializer the default serializer that would be used
     * @return a custom serializer if {@link JsonMapAsEntries} is present, otherwise the default serializer
     */
    @Override
    public JsonSerializer<?> modifyMapSerializer(SerializationConfig config, MapType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        JsonMapAsEntries jsonMapAsEntries = beanDesc.getClassAnnotations().get(JsonMapAsEntries.class);
        if (jsonMapAsEntries != null) {
            return createMapSerializer(config.getTypeFactory(), jsonMapAsEntries, valueType);
        }
        return super.modifyMapSerializer(config, valueType, beanDesc, serializer);
    }

    /**
     * Creates a custom serializer for maps based on the {@link JsonMapAsEntries} annotation configuration.
     * <p>
     * This method creates a serializer that converts a map into either:
     * <ul>
     *   <li>A list of {@link MapEntry} objects (when {@code entriesWrapped} is {@code false})</li>
     *   <li>A {@link WrappedMap} containing a list of entries (when {@code entriesWrapped} is {@code true})</li>
     * </ul>
     * <p>
     * The entry format (key as attribute or key as element) is determined by the
     * {@link JsonMapAsEntries#entryFormat()} value.
     *
     * @param typeFactory      the type factory for constructing Java types
     * @param jsonMapAsEntries the annotation containing serialization configuration
     * @param javaType         the Java type of the map (must be a {@link MapType})
     * @return a custom serializer for the map
     * @throws RuntimeException if the provided {@code javaType} is not a {@link MapType}
     */
    private JsonSerializer<Object> createMapSerializer(
            TypeFactory typeFactory, JsonMapAsEntries jsonMapAsEntries, JavaType javaType) {
        if (!(javaType instanceof MapType mapType)) {
            throw new RuntimeException("@JsonMapAsEntries only for map type");
        }
        MapToEntriesConverter<?, ?> conv = new MapToEntriesConverter<>(jsonMapAsEntries.entryFormat().getCls());
        JavaType mapEntryType = typeFactory.constructParametricType(jsonMapAsEntries.entryFormat().getCls(),
                mapType.getKeyType(), mapType.getContentType());

        if (jsonMapAsEntries.entriesWrapped()) {
            JavaType wrapperType = typeFactory.constructParametricType(WrappedMap.class,
                    mapEntryType, mapType.getKeyType(), mapType.getContentType());
            @SuppressWarnings("unchecked")
            Converter<Object, Object> castConv = (Converter<Object, Object>) ((Converter<?, ?>)
                    new WrappedMapToEntriesConverter<>(conv));
            return new TypedStdDelegatingSerializer(castConv, wrapperType);
        }

        JavaType type = typeFactory.constructCollectionType(List.class, mapEntryType);
        CollectionSerializer serializer1 = new CollectionSerializer(mapEntryType, true, null, null);
        @SuppressWarnings("unchecked")
        Converter<Object, Object> castConv = (Converter<Object, Object>) ((Converter<?, ?>) conv);
        return new StdDelegatingSerializer(castConv, type, serializer1);
    }
}

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
import io.github.dimkich.integration.testing.format.util.JacksonUtils;
import io.github.dimkich.integration.testing.format.util.MapTypes;

import java.util.List;

/**
 * A Jackson {@link BeanSerializerModifier} that customizes map serialization
 * for types and properties annotated with {@link JsonMapAsEntries}.
 * <p>
 * This modifier converts map values into a list of entry DTOs or a wrapped container
 * of entry DTOs, depending on annotation settings.
 * <p>
 * The modifier handles two scenarios:
 * <ul>
 *   <li><b>Field-level annotation:</b> {@link #changeProperties(SerializationConfig, BeanDescription, List)}
 *       assigns a custom serializer to annotated bean properties when their declared type can be resolved as a map.</li>
 *   <li><b>Type-level annotation:</b> {@link #modifyMapSerializer(SerializationConfig, MapType, BeanDescription, JsonSerializer)}
 *       returns a custom serializer for annotated map types.</li>
 * </ul>
 * <p>
 * The serialization behavior depends on the annotation configuration:
 * <ul>
 *   <li>If {@code entriesWrapped} is {@code false}, maps are serialized as a list of entries
 *       (for example, {@code [{"key":"k1","value":"v1"}, ...]}).</li>
 *   <li>If {@code entriesWrapped} is {@code true}, maps are serialized as a wrapper object
 *       containing entries (for example, {@code {"entries":[{"key":"k1","value":"v1"}, ...]}}).</li>
 * </ul>
 *
 * @see JsonMapAsEntries
 * @see BeanSerializerModifier
 */
public class MapAsEntriesSerializerModifier extends BeanSerializerModifier {
    /**
     * Applies custom map serialization to bean properties annotated with {@link JsonMapAsEntries}.
     * <p>
     * For each annotated property, this method attempts to build a delegating serializer.
     * If the property type cannot be resolved as a map, the property is left unchanged.
     *
     * @param config         the serialization configuration
     * @param beanDesc       the bean description containing metadata about the bean class
     * @param beanProperties the list of bean property writers to potentially modify
     * @return the same list instance with serializers assigned for supported properties
     */
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
        for (BeanPropertyWriter property : beanProperties) {
            JsonMapAsEntries jsonMapAsEntries = property.getAnnotation(JsonMapAsEntries.class);
            if (jsonMapAsEntries != null) {
                JsonSerializer<Object> ser = createMapSerializer(config.getTypeFactory(), jsonMapAsEntries, property.getType());
                if (ser != null) {
                    property.assignSerializer(ser);
                }
            }
        }
        return beanProperties;
    }

    /**
     * Replaces the serializer for map types annotated with {@link JsonMapAsEntries}.
     * <p>
     * If the annotation is present and a delegating serializer can be created, that serializer
     * is returned. Otherwise, the parent {@link BeanSerializerModifier} behavior is used.
     *
     * @param config     the serialization configuration
     * @param valueType  the map type being serialized
     * @param beanDesc   the bean description containing class annotations
     * @param serializer the serializer selected by Jackson before this modifier
     * @return a custom serializer for supported annotated map types, or the parent result
     */
    @Override
    public JsonSerializer<?> modifyMapSerializer(SerializationConfig config, MapType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        JsonMapAsEntries jsonMapAsEntries = beanDesc.getClassAnnotations().get(JsonMapAsEntries.class);
        if (jsonMapAsEntries != null) {
            JsonSerializer<Object> ser = createMapSerializer(config.getTypeFactory(), jsonMapAsEntries, valueType);
            if (ser != null) {
                return ser;
            }
        }
        return super.modifyMapSerializer(config, valueType, beanDesc, serializer);
    }

    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        return super.modifySerializer(config, beanDesc, serializer);
    }

    /**
     * Builds a delegating serializer for map-to-entry conversion based on {@link JsonMapAsEntries}.
     * <p>
     * Depending on {@link JsonMapAsEntries#entriesWrapped()}, this method converts maps into:
     * <ul>
     *   <li>a list of {@link MapEntry} objects, or</li>
     *   <li>a {@link WrappedMap} containing those entries.</li>
     * </ul>
     * The concrete entry DTO class is selected via {@link JsonMapAsEntries#entryFormat()}.
     *
     * @param typeFactory      the type factory for constructing Java types
     * @param jsonMapAsEntries the annotation containing serialization configuration
     * @param javaType         the declared property/type to resolve as a map
     * @return a delegating serializer, or {@code null} when {@code javaType} is not a supported map type
     */
    private JsonSerializer<Object> createMapSerializer(
            TypeFactory typeFactory, JsonMapAsEntries jsonMapAsEntries, JavaType javaType) {
        MapTypes types = JacksonUtils.resolveMapTypes(javaType);
        if (types == null) {
            return null;
        }

        MapToEntriesConverter<?, ?> conv = new MapToEntriesConverter<>(jsonMapAsEntries.entryFormat().getCls());
        JavaType mapEntryType = typeFactory.constructParametricType(jsonMapAsEntries.entryFormat().getCls(),
                types.getKeyType(), types.getValueType());

        if (jsonMapAsEntries.entriesWrapped()) {
            JavaType wrapperType = typeFactory.constructParametricType(WrappedMap.class,
                    mapEntryType, types.getKeyType(), types.getValueType());
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

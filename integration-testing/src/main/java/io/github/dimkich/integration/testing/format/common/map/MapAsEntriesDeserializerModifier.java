package io.github.dimkich.integration.testing.format.common.map;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import io.github.dimkich.integration.testing.format.common.map.converter.MapFromEntriesConverter;
import io.github.dimkich.integration.testing.format.common.map.converter.WrappedMapFromEntriesConverter;
import io.github.dimkich.integration.testing.format.common.map.dto.MapEntry;
import io.github.dimkich.integration.testing.format.common.map.dto.WrappedMap;
import io.github.dimkich.integration.testing.format.util.JacksonUtils;
import io.github.dimkich.integration.testing.format.util.MapTypes;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A Jackson {@link BeanDeserializerModifier} that processes {@link JsonMapAsEntries} annotations
 * to customize the deserialization of map types.
 * <p>
 * This modifier handles two scenarios:
 * <ul>
 *   <li><b>Field-level annotations:</b> When {@link JsonMapAsEntries} is applied to a map field,
 *       the modifier replaces the property's deserializer with a custom one that converts
 *       map entries into the target map type.</li>
 *   <li><b>Class-level annotations:</b> When {@link JsonMapAsEntries} is applied to a map class,
 *       the modifier replaces the map deserializer for that type.</li>
 * </ul>
 * <p>
 * The deserialization process supports two formats:
 * <ul>
 *   <li><b>Unwrapped entries:</b> When {@code entriesWrapped = false}, entries are deserialized
 *       as a {@link List} of {@link MapEntry} objects, then converted to a map.</li>
 *   <li><b>Wrapped entries:</b> When {@code entriesWrapped = true}, entries are deserialized
 *       as a {@link WrappedMap} container, then converted to a map.</li>
 * </ul>
 * <p>
 * The conversion from entries to map is performed by:
 * <ul>
 *   <li>{@link MapFromEntriesConverter} - converts a list of entries to a map</li>
 *   <li>{@link WrappedMapFromEntriesConverter} - converts a wrapped map container to a map</li>
 * </ul>
 *
 * @see JsonMapAsEntries
 * @see MapFromEntriesConverter
 * @see WrappedMapFromEntriesConverter
 */
public class MapAsEntriesDeserializerModifier extends BeanDeserializerModifier {
    /**
     * Processes bean properties annotated with {@link JsonMapAsEntries} and replaces their
     * deserializers with custom map deserializers that handle entry-based formats.
     * <p>
     * This method is called during bean deserializer building and scans all properties
     * for the {@link JsonMapAsEntries} annotation. When found, it creates a custom deserializer
     * that can handle the specified entry format (key as attribute or element) and wrapping
     * configuration.
     *
     * @param config   the deserialization configuration
     * @param beanDesc description of the bean being deserialized
     * @param builder  the builder used to construct the bean deserializer
     * @return the updated builder with modified property deserializers
     */
    @Override
    public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc, BeanDeserializerBuilder builder) {
        SettableBeanProperty[] args = builder.getValueInstantiator().getFromObjectArguments(config);
        for (Iterator<SettableBeanProperty> iterator = builder.getProperties(); iterator.hasNext(); ) {
            SettableBeanProperty property = iterator.next();
            JsonMapAsEntries jsonMapAsEntries = property.getAnnotation(JsonMapAsEntries.class);
            if (jsonMapAsEntries != null) {
                int index = find(args, property);
                JsonDeserializer<?> deser = createMapDeserializer(config.getTypeFactory(),
                        jsonMapAsEntries, property.getType());
                if (deser != null) {
                    property = property.withValueDeserializer(deser);
                    if (index >= 0) {
                        args[index] = property;
                    }
                    builder.addOrReplaceProperty(property, true);
                }
            }
        }

        return super.updateBuilder(config, beanDesc, builder);
    }

    /**
     * Finds the index of a property in the value instantiator arguments array.
     * <p>
     * This is used to update the constructor or factory method arguments when
     * a property's deserializer is modified.
     *
     * @param args     the array of properties used as instantiator arguments
     * @param property the property to find
     * @return the index of the property in the array, or -1 if not found
     */
    private int find(SettableBeanProperty[] args, SettableBeanProperty property) {
        if (args == null) {
            return -1;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] == property) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Modifies the deserializer for map types that have a class-level {@link JsonMapAsEntries} annotation.
     * <p>
     * When a map class is annotated with {@link JsonMapAsEntries}, this method creates a custom
     * deserializer that handles the entry-based format instead of the standard map deserialization.
     * <p>
     * This is used when the annotation is applied directly to a map class (e.g., a custom map
     * class extending {@link java.util.Map}), rather than to a specific field.
     *
     * @param config       the deserialization configuration
     * @param type         the map type being deserialized
     * @param beanDesc     description of the map class
     * @param deserializer the default map deserializer
     * @return a custom deserializer if the annotation is present and map types are resolvable;
     * otherwise delegates to the parent implementation
     */
    @Override
    public JsonDeserializer<?> modifyMapDeserializer(DeserializationConfig config, MapType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        JsonMapAsEntries jsonMapAsEntries = beanDesc.getClassAnnotations().get(JsonMapAsEntries.class);
        if (jsonMapAsEntries != null) {
            JsonDeserializer<?> deser = createMapDeserializer(config.getTypeFactory(), jsonMapAsEntries, type);
            if (deser != null) {
                return deser;
            }
        }

        return super.modifyMapDeserializer(config, type, beanDesc, deserializer);
    }

    /**
     * Creates a custom deserializer for a map type based on the {@link JsonMapAsEntries} annotation configuration.
     * <p>
     * The deserializer handles two scenarios:
     * <ul>
     *   <li><b>Wrapped entries:</b> When {@code entriesWrapped = true}, entries are deserialized
     *       as a {@link WrappedMap} container, then converted using {@link WrappedMapFromEntriesConverter}.</li>
     *   <li><b>Unwrapped entries:</b> When {@code entriesWrapped = false}, entries are deserialized
     *       as a {@link List} of {@link MapEntry} objects using a {@link CollectionDeserializer},
     *       then converted to a map using {@link MapFromEntriesConverter}.</li>
     * </ul>
     * <p>
     * The entry format (key as attribute or element) is determined by the annotation's
     * {@link JsonMapAsEntries#entryFormat()} value, which specifies the concrete
     * {@link MapEntry} implementation class to use.
     *
     * @param typeFactory      the type factory for constructing generic types
     * @param jsonMapAsEntries the annotation configuration
     * @param type             the map type being deserialized
     * @return a deserializer that converts entries to the target map type
     */
    private JsonDeserializer<?> createMapDeserializer(
            TypeFactory typeFactory, JsonMapAsEntries jsonMapAsEntries, JavaType type) {
        MapTypes types = JacksonUtils.resolveMapTypes(type);
        if (types == null) {
            return null;
        }
        MapFromEntriesConverter<?, ?> conv = new MapFromEntriesConverter<>(type.getRawClass());
        JavaType mapEntryType = typeFactory.constructParametricType(jsonMapAsEntries.entryFormat().getCls(),
                types.getKeyType(), types.getValueType());
        if (jsonMapAsEntries.entriesWrapped()) {
            JavaType wrapperType = typeFactory.constructParametricType(WrappedMap.class,
                    mapEntryType, types.getKeyType(), types.getValueType());

            @SuppressWarnings("unchecked")
            Converter<Object, Object> castConv = (Converter<Object, Object>) ((Converter<?, ?>)
                    new WrappedMapFromEntriesConverter<>(conv, wrapperType));

            return new StdDelegatingDeserializer<>(castConv, wrapperType, null);
        }
        CollectionType collectionType = typeFactory.constructCollectionType(List.class, mapEntryType);
        CollectionDeserializer listDeserializer = new CollectionDeserializer(collectionType, null, null,
                new CollectionInstantiator(collectionType.getRawClass()));
        @SuppressWarnings("unchecked")
        Converter<Object, Object> castConv = (Converter<Object, Object>) ((Converter<?, ?>) conv);
        return new StdDelegatingDeserializer<>(castConv, collectionType, listDeserializer);
    }

    private static class CollectionInstantiator extends ValueInstantiator.Base {
        private final Class<?> collectionClass;

        public CollectionInstantiator(Class<?> collectionClass) {
            super(collectionClass);
            this.collectionClass = collectionClass;
        }

        @Override
        public boolean canInstantiate() {
            return true;
        }

        @Override
        public boolean canCreateUsingDefault() {
            return true;
        }

        @Override
        @SneakyThrows
        public Object createUsingDefault(DeserializationContext ctxt) {
            if (collectionClass.isInterface()) {
                return new ArrayList<>();
            }
            return collectionClass.getDeclaredConstructor().newInstance();
        }
    }
}

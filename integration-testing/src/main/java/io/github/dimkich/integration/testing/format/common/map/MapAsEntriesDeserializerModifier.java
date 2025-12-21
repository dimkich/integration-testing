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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
                property = property.withValueDeserializer(createMapDeserializer(config.getTypeFactory(),
                        jsonMapAsEntries, property.getType()));
                if (index >= 0) {
                    args[index] = property;
                }
                builder.addOrReplaceProperty(property, true);
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
     * @return a custom deserializer if the annotation is present, otherwise the default deserializer
     */
    @Override
    public JsonDeserializer<?> modifyMapDeserializer(DeserializationConfig config, MapType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        JsonMapAsEntries jsonMapAsEntries = beanDesc.getClassAnnotations().get(JsonMapAsEntries.class);
        if (jsonMapAsEntries != null) {
            return createMapDeserializer(config.getTypeFactory(), jsonMapAsEntries, type);
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
     * @param <T>              the map type
     * @param <K>              the map key type
     * @param <V>              the map value type
     * @param typeFactory      the type factory for constructing generic types
     * @param jsonMapAsEntries the annotation configuration
     * @param type             the map type being deserialized
     * @return a deserializer that converts entries to the target map type
     * @throws RuntimeException if the type is not a {@link MapType}
     */
    private <T extends Map<K, V>, K, V> JsonDeserializer<?> createMapDeserializer(
            TypeFactory typeFactory, JsonMapAsEntries jsonMapAsEntries, JavaType type) {
        if (!(type instanceof MapType)) {
            throw new RuntimeException("@JsonMapAsEntries only for map type");
        }
        MapFromEntriesConverter<K, V> conv = new MapFromEntriesConverter<>(type.getRawClass());
        JavaType mapEntryType = typeFactory.constructParametricType(jsonMapAsEntries.entryFormat().getCls(),
                type.getKeyType(), type.getContentType());

        if (jsonMapAsEntries.entriesWrapped()) {
            JavaType wrapperType = typeFactory.constructParametricType(WrappedMap.class,
                    mapEntryType, type.getKeyType(), type.getContentType());
            @SuppressWarnings("unchecked")
            Converter<Object, T> castConv = (Converter<Object, T>) ((Converter<?, ?>)
                    new WrappedMapFromEntriesConverter<>(conv, wrapperType));
            return new StdDelegatingDeserializer<>(castConv, wrapperType, null
            );
        }
        CollectionType collectionType = typeFactory.constructCollectionType(List.class, mapEntryType);
        CollectionDeserializer deserializer = new CollectionDeserializer(collectionType, null,
                null, new ArrayListInstantiator());
        @SuppressWarnings("unchecked")
        Converter<Object, T> castConv = (Converter<Object, T>) ((Converter<?, ?>) conv);
        return new StdDelegatingDeserializer<>(castConv, collectionType, deserializer);
    }

    /**
     * A {@link ValueInstantiator} that creates new {@link ArrayList} instances during deserialization.
     * <p>
     * This is used by the {@link CollectionDeserializer} when deserializing unwrapped map entries
     * into a list before converting them to a map.
     */
    private static class ArrayListInstantiator extends ValueInstantiator.Base {
        /**
         * Creates a new instantiator for {@link ArrayList} instances.
         */
        public ArrayListInstantiator() {
            super(ArrayList.class);
        }

        /**
         * Indicates that this instantiator can create instances.
         *
         * @return always {@code true}
         */
        @Override
        public boolean canInstantiate() {
            return true;
        }

        /**
         * Indicates that this instantiator can create instances using the default constructor.
         *
         * @return always {@code true}
         */
        @Override
        public boolean canCreateUsingDefault() {
            return true;
        }

        /**
         * Creates a new empty {@link ArrayList} instance.
         *
         * @param ctxt the deserialization context
         * @return a new empty {@link ArrayList}
         */
        @Override
        public Object createUsingDefault(DeserializationContext ctxt) {
            return new ArrayList<>();
        }
    }
}

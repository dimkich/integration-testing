package io.github.dimkich.integration.testing.format.xml.map;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.type.TypeBindings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MapKeyDeserializerModifier extends BeanDeserializerModifier {
    @Override
    public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc, BeanDeserializerBuilder builder) {
        for (Iterator<SettableBeanProperty> iterator = builder.getProperties(); iterator.hasNext(); ) {
            SettableBeanProperty property = iterator.next();
            JsonMapKey jsonMapKey = property.getAnnotation(JsonMapKey.class);
            if (jsonMapKey != null) {
                property = property.withValueDeserializer(createMapDeserializer(jsonMapKey, property.getType()));
                builder.addOrReplaceProperty(property, true);
            }
        }
        return super.updateBuilder(config, beanDesc, builder);
    }

    @Override
    public JsonDeserializer<?> modifyMapDeserializer(DeserializationConfig config, MapType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        JsonMapKey jsonMapKey = beanDesc.getClassAnnotations().get(JsonMapKey.class);
        if (jsonMapKey != null) {
            return createMapDeserializer(jsonMapKey, type);
        }

        return super.modifyMapDeserializer(config, type, beanDesc, deserializer);
    }

    private JsonDeserializer<?> createMapDeserializer(JsonMapKey jsonMapKey, JavaType type) {
        if (!(type instanceof MapType mapType)) {
            throw new RuntimeException();
        }
        MapFromEntriesConverter conv = new MapFromEntriesConverter<>(type.getRawClass());
        SimpleType mapEntryType = new TypeWithBindings(jsonMapKey.value().getCls(), TypeBindings.create(
                MapEntryKeyAsAttribute.class, new JavaType[]{type.getKeyType(), type.getContentType()})
        );
        CollectionType collectionType = CollectionType.construct(List.class, mapEntryType);
        CollectionDeserializer deserializer1 = new CollectionDeserializer(collectionType, null, null, new ArrayListInstantiator());
        if (jsonMapKey.wrapped()) {
            JavaType type1 = new TypeWithBindings(WrappedMap.class, TypeBindings.create(
                    WrappedMap.class, new JavaType[]{mapEntryType, type.getKeyType(), type.getContentType()}));
            return new StdDelegatingDeserializer<Object>(new WrappedMapFromEntriesConverter<>(conv, type1), type1, null);
        }
        return new StdDelegatingDeserializer<Object>(conv, collectionType, deserializer1);
    }

    private static class ArrayListInstantiator extends ValueInstantiator.Base {
        public ArrayListInstantiator() {
            super(ArrayList.class);
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
        public Object createUsingDefault(DeserializationContext ctxt) {
            return new ArrayList<>();
        }
    }
}

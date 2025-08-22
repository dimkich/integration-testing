package io.github.dimkich.integration.testing.format.xml.map;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.CollectionSerializer;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import io.github.dimkich.integration.testing.format.common.factory.TypedStdDelegatingSerializer;

import java.util.List;

public class MapKeySerializerModifier extends BeanSerializerModifier {
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
        for (BeanPropertyWriter property : beanProperties) {
            JsonMapKey jsonMapKey = property.getAnnotation(JsonMapKey.class);
            if (jsonMapKey != null) {
                property.assignSerializer(createMapSerializer(jsonMapKey, property.getType()));
            }
        }
        return beanProperties;
    }

    @Override
    public JsonSerializer<?> modifyMapSerializer(SerializationConfig config, MapType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        JsonMapKey jsonMapKey = beanDesc.getClassAnnotations().get(JsonMapKey.class);
        if (jsonMapKey != null) {
            return createMapSerializer(jsonMapKey, valueType);
        }
        return super.modifyMapSerializer(config, valueType, beanDesc, serializer);
    }

    private JsonSerializer<Object> createMapSerializer(JsonMapKey jsonMapKey, JavaType javaType) {
        if (!(javaType instanceof MapType mapType)) {
            throw new RuntimeException();
        }
        MapToEntriesConverter conv = new MapToEntriesConverter<>(jsonMapKey.value().getCls());
        SimpleType mapEntryType = new TypeWithBindings(jsonMapKey.value().getCls(), TypeBindings.create(
                MapEntry.class, new JavaType[]{mapType.getKeyType(), mapType.getContentType()})
        );

        if (jsonMapKey.wrapped()) {
            JavaType type = new TypeWithBindings(WrappedMap.class, TypeBindings.create(
                    WrappedMap.class, new JavaType[]{mapEntryType, mapType.getKeyType(), mapType.getContentType()}));
            return new TypedStdDelegatingSerializer(new WrappedMapToEntriesConverter<>(conv), type);
        }

        JavaType type = CollectionType.construct(List.class, mapEntryType);
        CollectionSerializer serializer1 = new CollectionSerializer(mapEntryType, true, null, null);

        return new StdDelegatingSerializer(conv, type, serializer1);
    }
}

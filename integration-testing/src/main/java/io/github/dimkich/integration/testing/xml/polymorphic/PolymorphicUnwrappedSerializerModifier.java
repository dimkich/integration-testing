package io.github.dimkich.integration.testing.xml.polymorphic;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PolymorphicUnwrappedSerializerModifier extends BeanSerializerModifier {
    private final PolymorphicUnwrappedResolverBuilder typeResolverBuilder;

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
        for (int i = 0; i < beanProperties.size(); i++) {
            BeanPropertyWriter property = beanProperties.get(i);
            JsonUnwrapped jsonUnwrapped = property.getAnnotation(JsonUnwrapped.class);
            JacksonXmlText jacksonXmlText = property.getAnnotation(JacksonXmlText.class);
            if (((jsonUnwrapped != null && jsonUnwrapped.enabled()) || (jacksonXmlText != null && jacksonXmlText.value()))) {
                BeanPropertyWriter propertyWriter = beanProperties.get(i);
                TypeSerializer unwrappedTypeSerializer = propertyWriter.getTypeSerializer();
                propertyWriter.assignTypeSerializer(null);
                propertyWriter = new PolymorphicUnwrappedBeanPropertyWriter(propertyWriter, NameTransformer.NOP,
                        unwrappedTypeSerializer, typeResolverBuilder.getUnwrappedTypeProperty());
                beanProperties.set(i, propertyWriter);
            }
            if (property.getType().isCollectionLikeType()) {
                CollectionLikeType collectionType = (CollectionLikeType)property.getType();
                if (collectionType.getContentType().isCollectionLikeType()) {
                    beanProperties.set(i, new BeanPropertyWriterAsObject(property));
                }
            }
        }
        return beanProperties;
    }

    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        if (serializer instanceof StdSerializer<?> stdSerializer && !(serializer instanceof BeanSerializerBase)
                && !(serializer instanceof ContainerSerializer<?>)) {
            return new PolymorphicStdSerializer<>((StdSerializer<Object>) stdSerializer);
        }
        return super.modifySerializer(config, beanDesc, serializer);
    }

    @Override
    public JsonSerializer<?> modifyMapSerializer(SerializationConfig config, MapType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        JsonSerializer<?> ser = super.modifyMapSerializer(config, valueType, beanDesc, serializer);
        if (ser instanceof MapSerializer mapSerializer) {
            ser = new MapFixedSerializer(mapSerializer);
        }
        return ser;
    }
}

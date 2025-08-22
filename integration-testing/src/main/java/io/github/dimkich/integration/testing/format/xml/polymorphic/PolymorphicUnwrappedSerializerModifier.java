package io.github.dimkich.integration.testing.format.xml.polymorphic;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import io.github.dimkich.integration.testing.format.common.TestTypeResolverBuilder;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PolymorphicUnwrappedSerializerModifier extends BeanSerializerModifier {
    private final TestTypeResolverBuilder typeResolverBuilder;

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
    public JsonSerializer<?> modifyMapSerializer(SerializationConfig config, MapType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        JsonSerializer<?> ser = super.modifyMapSerializer(config, valueType, beanDesc, serializer);
        if (ser instanceof MapSerializer mapSerializer) {
            ser = new MapFixedSerializer(mapSerializer);
        }
        return ser;
    }
}

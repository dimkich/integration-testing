package io.github.dimkich.integration.testing.format.xml.polymorphic;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.impl.TypeDeserializerBase;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import io.github.dimkich.integration.testing.format.common.polymorphic.unwrapped.UnwrappedTypeProperty;
import io.github.dimkich.integration.testing.format.common.type.TestTypeResolverBuilder;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.Set;

@RequiredArgsConstructor
public class PolymorphicUnwrappedDeserializerModifier extends BeanDeserializerModifier {
    private final TestTypeResolverBuilder typeResolverBuilder;

    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc,
                                                  JsonDeserializer<?> deserializer) {
        if (!(deserializer instanceof BeanDeserializer beanDeserializer)) {
            return deserializer;
        }
        SettableBeanProperty unwrappedProperty = findUnwrappedProperty(beanDeserializer);
        if (unwrappedProperty == null) {
            return beanDeserializer;
        }

        String typePropertyName = findTypePropertyName(unwrappedProperty, beanDesc);
        TypeDeserializerBase unwrappedTypeDeserializer;
        if (!(unwrappedProperty.getValueTypeDeserializer() instanceof TypeDeserializerBase utd)) {
            unwrappedTypeDeserializer = null;
        } else {
            unwrappedTypeDeserializer = new AsPropertyTypeDeserializer(
                    utd.baseType(),
                    utd.getTypeIdResolver(),
                    typePropertyName,
                    false,
                    null,
                    utd.getTypeInclusion(),
                    true
            );
        }
        return new PolymorphicUnwrappedDeserializer(beanDeserializer, Set.of("", unwrappedProperty.getName()),
                unwrappedProperty, unwrappedTypeDeserializer,
                unwrappedTypeDeserializer == null ? null : typePropertyName);
    }

    private SettableBeanProperty findUnwrappedProperty(BeanDeserializerBase deserializer) {
        for (Iterator<SettableBeanProperty> properties = deserializer.properties(); properties.hasNext(); ) {
            SettableBeanProperty property = properties.next();
            JsonUnwrapped jsonUnwrapped = property.getAnnotation(JsonUnwrapped.class);
            JacksonXmlText jacksonXmlText = property.getAnnotation(JacksonXmlText.class);
            if ((jsonUnwrapped != null && jsonUnwrapped.enabled())
                    || (jacksonXmlText != null && jacksonXmlText.value())) {
                return property;
            }
        }
        return null;
    }

    private String findTypePropertyName(SettableBeanProperty property, BeanDescription beanDesc) {
        UnwrappedTypeProperty annotation = property.getAnnotation(UnwrappedTypeProperty.class);
        if (annotation == null) {
            annotation = beanDesc.getClassAnnotations().get(UnwrappedTypeProperty.class);
        }
        return annotation == null ? typeResolverBuilder.getUnwrappedTypeProperty() : annotation.value();
    }
}

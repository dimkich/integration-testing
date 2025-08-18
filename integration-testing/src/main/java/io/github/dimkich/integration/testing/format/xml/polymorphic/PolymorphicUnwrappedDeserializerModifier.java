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
import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.Set;

@RequiredArgsConstructor
public class PolymorphicUnwrappedDeserializerModifier extends BeanDeserializerModifier {
    private final PolymorphicUnwrappedResolverBuilder typeResolverBuilder;

    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deser) {
        if (!(deser instanceof BeanDeserializerBase)) {
            return deser;
        }
        BeanDeserializer deserializer = (BeanDeserializer) deser;
        SettableBeanProperty unwrappedProperty = null;
        for (Iterator<SettableBeanProperty> properties = deserializer.properties(); properties.hasNext(); ) {
            SettableBeanProperty property = properties.next();
            JsonUnwrapped jsonUnwrapped = property.getAnnotation(JsonUnwrapped.class);
            JacksonXmlText jacksonXmlText = property.getAnnotation(JacksonXmlText.class);
            if ((jsonUnwrapped != null && jsonUnwrapped.enabled()) || (jacksonXmlText != null && jacksonXmlText.value())) {
                unwrappedProperty = property;

                TypeDeserializerBase unwrappedTypeDeserializer;
                if (!(unwrappedProperty.getValueTypeDeserializer() instanceof TypeDeserializerBase utd)) {
                    unwrappedTypeDeserializer = null;
                } else {
                    unwrappedTypeDeserializer = new AsPropertyTypeDeserializer(
                            utd.baseType(),
                            utd.getTypeIdResolver(),
                            typeResolverBuilder.getUnwrappedTypeProperty(),
                            false,
                            null,
                            utd.getTypeInclusion()
                    );
                }
                deserializer = new PolymorphicUnwrappedDeserializer(deserializer, Set.of("", unwrappedProperty.getName()),
                        unwrappedProperty, unwrappedTypeDeserializer);
                break;
            }
        }
        return deserializer;
    }
}

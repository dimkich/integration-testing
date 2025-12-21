package io.github.dimkich.integration.testing.format.xml.attributes;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import io.github.dimkich.integration.testing.format.common.type.TestTypeResolverBuilder;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;

@RequiredArgsConstructor
public class BeanAsAttributesDeserializerModifier  extends BeanDeserializerModifier {
    private final TestTypeResolverBuilder builder;

    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deser) {
        if (!(deser instanceof BeanDeserializerBase)) {
            return deser;
        }
        BeanDeserializer deserializer = (BeanDeserializer) deser;
        for (Iterator<SettableBeanProperty> iterator = deserializer.properties(); iterator.hasNext(); ) {
            SettableBeanProperty property = iterator.next();
            BeanAsAttributes beanAsAttributes = property.getAnnotation(BeanAsAttributes.class);
            if (beanAsAttributes != null && beanAsAttributes.enabled()) {
                return new BeanAsAttributesDeserializer(deserializer, property, builder.getTypeAttributes());
            }
        }

        return deser;
    }
}

package io.github.dimkich.integration.testing.xml.attributes;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.Set;

@RequiredArgsConstructor
public class BeanAsAttributesDeserializerModifier  extends BeanDeserializerModifier {
    private final Set<String> typeAttributes;

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
                return new BeanAsAttributesDeserializer(deserializer, property, typeAttributes);
            }
        }

        return deser;
    }
}

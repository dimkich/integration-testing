package io.github.dimkich.integration.testing.format.common.location;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import io.github.dimkich.integration.testing.Test;

public class StoreLocationDeserializerModifier extends BeanDeserializerModifier {
    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        JsonDeserializer<?> deser = super.modifyDeserializer(config, beanDesc, deserializer);
        if (Test.class.isAssignableFrom(beanDesc.getBeanClass())) {
            deser = new StoreLocationDeserializer(deser);
        }
        return deser;
    }
}

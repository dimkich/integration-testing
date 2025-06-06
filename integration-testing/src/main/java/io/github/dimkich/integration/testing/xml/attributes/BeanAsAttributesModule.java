package io.github.dimkich.integration.testing.xml.attributes;

import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.Set;

public class BeanAsAttributesModule extends SimpleModule {
    public BeanAsAttributesModule(Set<String> typeAttributes) {
        setSerializerModifier(new BeanAsAttributesSerializerModifier());
        setDeserializerModifier(new BeanAsAttributesDeserializerModifier(typeAttributes));
    }
}

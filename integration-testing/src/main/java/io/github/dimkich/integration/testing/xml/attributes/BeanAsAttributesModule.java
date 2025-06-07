package io.github.dimkich.integration.testing.xml.attributes;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.dimkich.integration.testing.xml.polymorphic.PolymorphicUnwrappedResolverBuilder;

import java.util.Set;

public class BeanAsAttributesModule extends SimpleModule {
    public BeanAsAttributesModule(PolymorphicUnwrappedResolverBuilder builder) {
        setSerializerModifier(new BeanAsAttributesSerializerModifier());
        setDeserializerModifier(new BeanAsAttributesDeserializerModifier(builder));
    }
}

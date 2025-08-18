package io.github.dimkich.integration.testing.format.xml.polymorphic;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class PolymorphicUnwrappedModule extends SimpleModule {
    public PolymorphicUnwrappedModule(PolymorphicUnwrappedResolverBuilder builder) {
        super();
        setSerializerModifier(new PolymorphicUnwrappedSerializerModifier(builder));
        setDeserializerModifier(new PolymorphicUnwrappedDeserializerModifier(builder));
    }
}

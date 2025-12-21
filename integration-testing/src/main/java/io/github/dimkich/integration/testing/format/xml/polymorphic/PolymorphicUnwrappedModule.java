package io.github.dimkich.integration.testing.format.xml.polymorphic;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.dimkich.integration.testing.format.common.type.TestTypeResolverBuilder;

public class PolymorphicUnwrappedModule extends SimpleModule {
    public PolymorphicUnwrappedModule(TestTypeResolverBuilder builder) {
        super();
        setSerializerModifier(new PolymorphicUnwrappedSerializerModifier(builder));
        setDeserializerModifier(new PolymorphicUnwrappedDeserializerModifier(builder));
    }
}

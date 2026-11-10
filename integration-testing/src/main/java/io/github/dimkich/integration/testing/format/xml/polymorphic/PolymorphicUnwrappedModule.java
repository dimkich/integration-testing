package io.github.dimkich.integration.testing.format.xml.polymorphic;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.dimkich.integration.testing.config.SimpleTypeDetector;
import io.github.dimkich.integration.testing.format.common.type.TestTypeResolverBuilder;

public class PolymorphicUnwrappedModule extends SimpleModule {
    public PolymorphicUnwrappedModule(TestTypeResolverBuilder builder, SimpleTypeDetector simpleTypeDetector) {
        super();
        setSerializerModifier(new PolymorphicUnwrappedSerializerModifier(builder, simpleTypeDetector));
        setDeserializerModifier(new PolymorphicUnwrappedDeserializerModifier(builder));
    }
}

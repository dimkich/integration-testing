package io.github.dimkich.integration.testing.format.xml.attributes;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.dimkich.integration.testing.format.common.TestTypeResolverBuilder;

public class BeanAsAttributesModule extends SimpleModule {
    public BeanAsAttributesModule(TestTypeResolverBuilder builder) {
        setSerializerModifier(new BeanAsAttributesSerializerModifier());
        setDeserializerModifier(new BeanAsAttributesDeserializerModifier(builder));
    }
}

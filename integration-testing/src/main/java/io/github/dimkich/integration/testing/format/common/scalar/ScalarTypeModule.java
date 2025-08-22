package io.github.dimkich.integration.testing.format.common.scalar;

import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.Set;

public class ScalarTypeModule extends SimpleModule {
    public ScalarTypeModule(Set<Class<?>> classes) {
        super();
        setSerializerModifier(new ScalarTypeSerializerModifier(classes));
    }
}

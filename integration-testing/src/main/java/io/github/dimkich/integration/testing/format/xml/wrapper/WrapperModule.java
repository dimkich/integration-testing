package io.github.dimkich.integration.testing.format.xml.wrapper;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class WrapperModule extends SimpleModule {
    public WrapperModule() {
        super();
        setSerializerModifier(new WrappedSerializerModifier());
    }
}

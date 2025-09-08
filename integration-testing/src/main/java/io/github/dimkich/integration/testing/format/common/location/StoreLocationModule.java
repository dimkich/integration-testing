package io.github.dimkich.integration.testing.format.common.location;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class StoreLocationModule extends SimpleModule {
    public StoreLocationModule() {
        super();
        setDeserializerModifier(new StoreLocationDeserializerModifier());
    }
}

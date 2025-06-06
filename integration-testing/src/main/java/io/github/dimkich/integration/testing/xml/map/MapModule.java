package io.github.dimkich.integration.testing.xml.map;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class MapModule  extends SimpleModule {
    public MapModule() {
        super();
        setSerializerModifier(new MapKeySerializerModifier());
        setDeserializerModifier(new MapKeyDeserializerModifier());
    }
}

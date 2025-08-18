package io.github.dimkich.integration.testing.format.xml.map;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class MapModule  extends SimpleModule {
    public MapModule() {
        super();
        setSerializerModifier(new MapKeySerializerModifier());
        setDeserializerModifier(new MapKeyDeserializerModifier());
    }
}

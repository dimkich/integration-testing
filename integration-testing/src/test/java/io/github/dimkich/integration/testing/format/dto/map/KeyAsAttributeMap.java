package io.github.dimkich.integration.testing.format.dto.map;

import java.util.Map;

public class KeyAsAttributeMap extends KeyAsAttributeBase<Map<String, String>> {
    public static KeyAsAttributeMap of(Map<String, String> map) {
        KeyAsAttributeMap r = new KeyAsAttributeMap();
        r.setVal(map);
        return r;
    }
}

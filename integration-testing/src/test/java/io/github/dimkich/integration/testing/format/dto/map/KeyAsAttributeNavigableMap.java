package io.github.dimkich.integration.testing.format.dto.map;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class KeyAsAttributeNavigableMap extends KeyAsAttributeBase<NavigableMap<String, String>> {
    public static KeyAsAttributeNavigableMap of(Map<String, String> map) {
        KeyAsAttributeNavigableMap r = new KeyAsAttributeNavigableMap();
        r.setVal(new TreeMap<>(map));
        return r;
    }
}
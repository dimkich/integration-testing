package io.github.dimkich.integration.testing.format.dto.map;

import java.util.Map;

public class KeyAsAttributeIterable extends KeyAsAttributeBase<Iterable<Map.Entry<String, String>>> {
    public static KeyAsAttributeIterable of(Iterable<Map.Entry<String, String>> val) {
        KeyAsAttributeIterable r = new KeyAsAttributeIterable();
        r.setVal(val);
        return r;
    }
}
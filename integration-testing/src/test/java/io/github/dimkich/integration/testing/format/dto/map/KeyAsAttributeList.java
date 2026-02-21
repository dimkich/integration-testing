package io.github.dimkich.integration.testing.format.dto.map;

import java.util.List;
import java.util.Map;

public class KeyAsAttributeList extends KeyAsAttributeBase<List<Map.Entry<String, String>>> {
    public static KeyAsAttributeList of(List<Map.Entry<String, String>> val) {
        KeyAsAttributeList r = new KeyAsAttributeList();
        r.setVal(val);
        return r;
    }
}


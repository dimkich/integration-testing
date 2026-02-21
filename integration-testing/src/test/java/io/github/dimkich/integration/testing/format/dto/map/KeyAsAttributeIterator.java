package io.github.dimkich.integration.testing.format.dto.map;

import java.util.Iterator;
import java.util.Map;

public class KeyAsAttributeIterator extends KeyAsAttributeBase<Iterator<Map.Entry<String, String>>> {
    public static KeyAsAttributeIterator of(Iterator<Map.Entry<String, String>> val) {
        KeyAsAttributeIterator r = new KeyAsAttributeIterator();
        r.setVal(val);
        return r;
    }
}
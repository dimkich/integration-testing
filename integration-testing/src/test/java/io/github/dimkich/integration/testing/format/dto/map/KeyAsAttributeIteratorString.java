package io.github.dimkich.integration.testing.format.dto.map;

import java.util.Iterator;
import java.util.List;

public class KeyAsAttributeIteratorString extends KeyAsAttributeBase<Iterator<String>> {
    public static KeyAsAttributeIteratorString of(List<String> val) {
        KeyAsAttributeIteratorString r = new KeyAsAttributeIteratorString();
        r.setVal(val.iterator());
        return r;
    }
}
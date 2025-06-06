package io.github.dimkich.integration.testing.xml.map;

import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.type.TypeBindings;

public class TypeWithBindings extends SimpleType {
    public TypeWithBindings(Class<?> cls, TypeBindings bindings) {
        super(cls, bindings, null, null);
    }
}

package io.github.dimkich.integration.testing.format.common.type.synthetic;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

@Data
@RequiredArgsConstructor
public class SyntheticGenericArrayType implements GenericArrayType {
    public final Type genericComponentType;

    @Override
    public boolean equals(Object o) {
        return o instanceof GenericArrayType that && genericComponentType.equals(that.getGenericComponentType());
    }

    @Override
    public String toString() {
        return genericComponentType + "[]";
    }
}

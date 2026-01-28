package io.github.dimkich.integration.testing.format.common.type.synthetic;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

@Data
@RequiredArgsConstructor
/**
 * Synthetic implementation of {@link GenericArrayType} created at runtime.
 * <p>
 * This type is used to represent array types whose component type is described
 * by a generic {@link Type} rather than by a concrete {@link Class} instance.
 */
public class SyntheticGenericArrayType implements GenericArrayType {
    /**
     * Generic component type of the synthetic array.
     */
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

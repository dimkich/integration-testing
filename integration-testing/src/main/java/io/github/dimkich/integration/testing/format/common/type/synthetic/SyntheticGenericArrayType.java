package io.github.dimkich.integration.testing.format.common.type.synthetic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Synthetic implementation of {@link GenericArrayType} created at runtime.
 * <p>
 * This type is used to represent array types whose component type is described
 * by a generic {@link Type} rather than by a concrete {@link Class} instance.
 */
@Getter
@RequiredArgsConstructor
public class SyntheticGenericArrayType implements GenericArrayType {
    /**
     * Generic component type of the synthetic array.
     */
    public final Type genericComponentType;

    /**
     * Compares this generic array type with the specified object for equality.
     * <p>
     * Two {@link GenericArrayType} instances are considered equal if their
     * generic component types are equal.
     * </p>
     *
     * @param o the object to be compared for equality with this type
     * @return {@code true} if the specified object is a {@link GenericArrayType}
     * with an equivalent component type
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof GenericArrayType that && genericComponentType.equals(that.getGenericComponentType());
    }

    /**
     * Returns a hash code value for this generic array type.
     * <p>
     * The hash code of a {@link GenericArrayType} is defined as the hash code
     * of its component type.
     * </p>
     *
     * @return the hash code value for this type
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(genericComponentType);
    }

    /**
     * Returns a string representation of this generic array type.
     * <p>
     * The format follows the Java convention: the component type's name
     * followed by "[]".
     * </p>
     *
     * @return a string representation of this type
     */
    @Override
    public String toString() {
        return genericComponentType + "[]";
    }
}

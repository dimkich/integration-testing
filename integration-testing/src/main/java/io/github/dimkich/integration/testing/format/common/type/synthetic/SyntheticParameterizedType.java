package io.github.dimkich.integration.testing.format.common.type.synthetic;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

@Data
@RequiredArgsConstructor
/**
 * Synthetic implementation of {@link ParameterizedType} that can be used to create
 * parameterized type instances at runtime, for example for testing or reflective
 * type construction.
 */
public class SyntheticParameterizedType implements ParameterizedType {
    /**
     * Raw type of this parameterized type, e.g. {@code List}.
     */
    private final Type rawType;

    /**
     * Actual type arguments for this type, e.g. {@code String} for {@code List<String>}.
     */
    private final Type[] actualTypeArguments;

    /**
     * Always returns {@code null} because this synthetic type does not model owner types.
     */
    public Type getOwnerType() {
        return null;
    }

    /**
     * Returns a canonical string representation of this parameterized type, including its
     * raw type and actual type arguments, e.g. {@code "java.util.List<java.lang.String>"}.
     */
    @Override
    public String getTypeName() {
        String typeName = this.rawType.getTypeName();
        if (this.actualTypeArguments.length > 0) {
            StringJoiner stringJoiner = new StringJoiner(", ", "<", ">");
            for (Type argument : this.actualTypeArguments) {
                stringJoiner.add(argument.getTypeName());
            }
            return typeName + stringJoiner;
        }
        return typeName;
    }

    /**
     * Compares this synthetic type with another {@link ParameterizedType} based on raw type,
     * owner type (always {@code null} for this implementation) and actual type arguments.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof ParameterizedType that) {
            return Objects.equals(null, that.getOwnerType()) && Objects.equals(rawType, that.getRawType())
                    && Arrays.equals(actualTypeArguments, that.getActualTypeArguments());
        } else {
            return false;
        }
    }

    /**
     * Returns the same value as {@link #getTypeName()}.
     */
    @Override
    public String toString() {
        return getTypeName();
    }
}

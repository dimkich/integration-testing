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
public class SyntheticParameterizedType implements ParameterizedType {
    private final Type rawType;
    private final Type[] actualTypeArguments;

    public Type getOwnerType() {
        return null;
    }

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

    @Override
    public boolean equals(Object o) {
        if (o instanceof ParameterizedType that) {
            return Objects.equals(null, that.getOwnerType()) && Objects.equals(rawType, that.getRawType())
                    && Arrays.equals(actualTypeArguments, that.getActualTypeArguments());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return getTypeName();
    }
}

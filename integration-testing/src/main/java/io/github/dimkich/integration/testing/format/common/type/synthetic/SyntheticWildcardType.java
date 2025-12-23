package io.github.dimkich.integration.testing.format.common.type.synthetic;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

@Data
@RequiredArgsConstructor
public class SyntheticWildcardType implements WildcardType {
    private final Type[] upperBounds;
    private final Type[] lowerBounds;

    @Override
    public boolean equals(Object object) {
        if (object instanceof WildcardType type) {
            return Arrays.equals(this.upperBounds, type.getUpperBounds())
                    && Arrays.equals(this.lowerBounds, type.getLowerBounds());
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("?");
        if (lowerBounds != null && lowerBounds.length > 0) {
            sb.append(" super ");
            for (int i = 0; i < lowerBounds.length; i++) {
                if (i > 0) sb.append(" & ");
                sb.append(lowerBounds[i].getTypeName());
            }
        } else if (upperBounds != null && upperBounds.length > 0) {
            boolean hasNonObjectBound = false;
            for (Type bound : upperBounds) {
                if (!Object.class.equals(bound)) {
                    hasNonObjectBound = true;
                    break;
                }
            }
            if (hasNonObjectBound) {
                sb.append(" extends ");
                for (int i = 0; i < upperBounds.length; i++) {
                    if (i > 0) sb.append(" & ");
                    sb.append(upperBounds[i].getTypeName());
                }
            }
        }
        return sb.toString();
    }
}

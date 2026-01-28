package io.github.dimkich.integration.testing.format.common.type.synthetic;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

/**
 * Synthetic implementation of {@link WildcardType} used to represent
 * wildcard type arguments with explicitly defined upper and lower bounds.
 *
 * <p>The bounds are provided at construction time and exposed via the
 * Lombok-generated accessors for {@code upperBounds} and {@code lowerBounds}.</p>
 */
@Data
@RequiredArgsConstructor
public class SyntheticWildcardType implements WildcardType {

    /**
     * Upper bounds of this wildcard type. By convention, a missing upper bound
     * is represented as a single-element array containing {@link Object}.
     */
    private final Type[] upperBounds;

    /**
     * Lower bounds of this wildcard type. An empty array or {@code null}
     * indicates that no lower bound is specified.
     */
    private final Type[] lowerBounds;

    /**
     * Compares this wildcard type with the supplied object by delegating to
     * the {@link WildcardType#getUpperBounds()} and
     * {@link WildcardType#getLowerBounds()} contracts.
     *
     * @param object other object to compare with
     * @return {@code true} if the other object is a {@link WildcardType} with
     * the same upper and lower bounds, {@code false} otherwise
     */
    @Override
    public boolean equals(Object object) {
        if (object instanceof WildcardType type) {
            return Arrays.equals(this.upperBounds, type.getUpperBounds())
                    && Arrays.equals(this.lowerBounds, type.getLowerBounds());
        }
        return false;
    }

    /**
     * Returns a string representation of this wildcard type that follows the
     * standard Java syntax:
     * <ul>
     *     <li>{@code "?"} for an unbounded wildcard</li>
     *     <li>{@code "? extends Bound"} when upper bounds are specified
     *     and at least one of them is not {@link Object}</li>
     *     <li>{@code "? super Bound"} when lower bounds are specified</li>
     * </ul>
     *
     * @return human-readable representation of this wildcard type
     */
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

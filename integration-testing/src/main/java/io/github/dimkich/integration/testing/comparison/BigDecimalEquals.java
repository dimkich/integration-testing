package io.github.dimkich.integration.testing.comparison;

import java.math.BigDecimal;
import java.util.function.BiPredicate;

/**
 * Compares {@link BigDecimal} values by value, ignoring scale.
 */
public class BigDecimalEquals implements BiPredicate<BigDecimal, BigDecimal> {
    @Override
    public boolean test(BigDecimal actual, BigDecimal expected) {
        return actual.compareTo(expected) == 0;
    }
}

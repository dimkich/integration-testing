package io.github.dimkich.integration.testing.comparison;

import java.time.ZonedDateTime;
import java.util.function.BiPredicate;

/**
 * Compares {@link ZonedDateTime} values by instant, ignoring the zone offset.
 */
public class ZonedDateTimeEquals implements BiPredicate<ZonedDateTime, ZonedDateTime> {
    @Override
    public boolean test(ZonedDateTime actual, ZonedDateTime expected) {
        return actual.toInstant().equals(expected.toInstant());
    }
}

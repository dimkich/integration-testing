package io.github.dimkich.integration.testing;

import java.time.ZonedDateTime;

/**
 * Abstraction for setting the current "now" moment used in tests.
 * <p>
 * Implementations typically update a clock or other time source
 * so that code under test observes the provided {@link ZonedDateTime}.
 * </p>
 */
public interface NowSetter {
    /**
     * Sets the current "now" value that should be used by the system under test.
     *
     * @param dateTime new moment in time that will be treated as the current time
     */
    void setNow(ZonedDateTime dateTime);
}

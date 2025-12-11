package io.github.dimkich.integration.testing.initialization.date.time;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.initialization.TestInit;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * Configuration for initializing date/time in integration tests.
 * This class allows setting an absolute date/time or adding a duration to the current time,
 * which is useful for testing time-dependent functionality.
 * <p>
 * The date/time initialization can be applied to specific test types using the {@code applyTo}
 * attribute inherited from {@link TestInit}.
 * <p>
 * XML Examples:
 * <pre>{@code
 * <!-- Set an absolute date/time -->
 * <init type="dateTimeInit" dateTime="2025-01-01T00:00:00Z"/>
 *
 * <!-- Set date/time with applyTo attribute -->
 * <init type="dateTimeInit" applyTo="TestCase" dateTime="2025-01-01T00:00:00Z"/>
 *
 * <!-- Add duration to current time (increments by 1 minute) -->
 * <init type="dateTimeInit" applyTo="TestPart" addDuration="PT1M"/>
 *
 * <!-- Combine absolute date/time with duration increment -->
 * <init type="dateTimeInit" applyTo="TestCase" dateTime="2025-01-01T00:00:00Z"/>
 * <init type="dateTimeInit" applyTo="TestPart" addDuration="PT1M"/>
 * }</pre>
 *
 * @see TestInit
 */
@Getter
@Setter
@ToString
public class DateTimeInit extends TestInit {
    /**
     * The absolute date/time to set. If specified, this will be the base time used.
     * If {@code addDuration} is also specified, the duration will be added to this date/time.
     * If not specified and {@code addDuration} is provided, the duration will be added to the current time.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="dateTimeInit" dateTime="2025-01-01T00:00:00Z"/>
     * }</pre>
     */
    @JacksonXmlProperty(isAttribute = true)
    private ZonedDateTime dateTime;

    /**
     * Duration to add to the current or specified date/time.
     * If {@code dateTime} is specified, the duration will be added to that date/time.
     * If {@code dateTime} is not specified, the duration will be added to the current time.
     * <p>
     * Duration format follows ISO-8601 duration format (e.g., PT1M for 1 minute, PT2H30M for 2 hours 30 minutes).
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="dateTimeInit" addDuration="PT1M"/>
     * }</pre>
     */
    @JacksonXmlProperty(isAttribute = true)
    private Duration addDuration;
}

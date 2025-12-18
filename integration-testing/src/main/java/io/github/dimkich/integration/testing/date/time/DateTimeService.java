package io.github.dimkich.integration.testing.date.time;

import io.github.dimkich.integration.testing.NowSetter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Service for controlling the current date-time value used in tests.
 * <p>
 * The service keeps an internal {@link ZonedDateTime} value and propagates it
 * to all configured {@link NowSetter} instances, which are responsible for
 * updating the underlying date/time providers (e.g. {@code Clock}, static helpers, etc.).
 */
@RequiredArgsConstructor
public class DateTimeService {

    /**
     * Registered {@link NowSetter} instances which will be updated whenever the current
     * date-time changes.
     */
    private final List<NowSetter> nowSetters;

    /**
     * Current date-time value managed by this service.
     */
    @Getter
    private ZonedDateTime dateTime;

    /**
     * Sets the current date-time.
     * <p>
     * The new value is stored internally and propagated to all {@link NowSetter} instances.
     *
     * @param dateTime new current date-time value; must not be {@code null}
     */
    public void setNow(ZonedDateTime dateTime) {
        this.dateTime = dateTime;
        nowSetters.forEach(s -> s.setNow(dateTime));
    }

    /**
     * Adds the specified duration to the current date-time and updates all registered
     * {@link NowSetter} instances.
     *
     * @param duration duration to add to the current date-time; must not be {@code null}
     * @throws RuntimeException if the service has not been initialized via {@link #setNow(ZonedDateTime)}
     */
    public void addDuration(Duration duration) {
        if (dateTime == null) {
            throw new RuntimeException("Service is not initialized");
        }
        setNow(dateTime.plus(duration));
    }
}

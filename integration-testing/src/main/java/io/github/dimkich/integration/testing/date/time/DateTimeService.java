package io.github.dimkich.integration.testing.date.time;

import io.github.dimkich.integration.testing.NowSetter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@RequiredArgsConstructor
public class DateTimeService {

    /** Number of full setter passes attempted before a failure is propagated. */
    private static final int MAX_ATTEMPTS = 3;

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
     * All setters are invoked on every pass; if at least one of them fails, the full pass is
     * retried up to {@value #MAX_ATTEMPTS} times so that transient failures (for example, a
     * Redis replication hiccup) do not leave the time sources out of sync. The last failure is
     * thrown when all attempts fail.
     *
     * @param dateTime new current date-time value; must not be {@code null}
     */
    public void setNow(ZonedDateTime dateTime) {
        this.dateTime = dateTime;
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            lastError = null;
            for (NowSetter setter : nowSetters) {
                try {
                    setter.setNow(dateTime);
                } catch (RuntimeException e) {
                    if (lastError == null) {
                        lastError = e;
                    } else {
                        lastError.addSuppressed(e);
                    }
                }
            }
            if (lastError == null) {
                return;
            }
            log.warn("Applying date-time {} attempt {}/{} failed, retrying", dateTime, attempt, MAX_ATTEMPTS, lastError);
        }
        throw lastError;
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

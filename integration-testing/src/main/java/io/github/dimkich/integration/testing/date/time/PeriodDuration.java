package io.github.dimkich.integration.testing.date.time;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Duration;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Locale;

/**
 * An immutable value type combining {@link Period} and {@link Duration} to represent
 * a temporal amount with both date-based (years, months, days) and time-based
 * (hours, minutes, seconds, nanos) components.
 * <p>
 * The representation is normalized: negative durations are converted to negative days
 * in the period, and durations of 24 hours or more have whole days extracted into
 * the period. The time component is always in the range [0, 24) hours.
 *
 * @see Period
 * @see Duration
 */
@Getter
@EqualsAndHashCode
public class PeriodDuration {
    /** Constant for a period-duration of zero. */
    public static final PeriodDuration ZERO = new PeriodDuration(Period.ZERO, Duration.ZERO);

    /** The date-based component (years, months, days). */
    private final Period period;

    /** The time-based component (hours, minutes, seconds, nanos), always in [0, 24) hours. */
    private final Duration duration;

    private PeriodDuration(Period period, Duration duration) {
        period = period == null ? Period.ZERO : period;
        duration = duration == null ? Duration.ZERO : duration;
        if (duration.isNegative()) {
            period = period.minusDays(1);
            duration = duration.plusDays(1);
        }
        if (duration.toHours() >= 24) {
            period = period.plusDays(duration.toDays());
            duration = duration.minusDays(duration.toDays());
        }
        this.period = period.normalized();
        this.duration = duration;
    }

    /**
     * Creates a period-duration from the given period and duration.
     *
     * @param p the period (date-based component); may be null, treated as zero
     * @param d the duration (time-based component); may be null, treated as zero
     * @return a normalized period-duration
     */
    public static PeriodDuration of(Period p, Duration d) {
        return new PeriodDuration(p, d);
    }

    /**
     * Creates a period-duration from the given duration only (with zero period).
     *
     * @param d the duration; may be null, treated as zero
     * @return a normalized period-duration
     */
    public static PeriodDuration of(Duration d) {
        return new PeriodDuration(Period.ZERO, d);
    }

    /**
     * Obtains a period-duration representing the time between two zoned date-times.
     *
     * @param start the start (inclusive)
     * @param end   the end (exclusive)
     * @return the period-duration between start and end, or {@link #ZERO} if end is not after start
     */
    public static PeriodDuration between(ZonedDateTime start, ZonedDateTime end) {
        if (!end.isAfter(start)) {
            return ZERO;
        }
        Period p = Period.between(start.toLocalDate(), end.toLocalDate());
        Duration d = Duration.between(start.plus(p), end);

        return new PeriodDuration(p.normalized(), d);
    }

    /**
     * Parses a period-duration from the given text in ISO-8601 format.
     * <p>
     * Supports: PnYnMnD (period only), PTnHnMnS (duration only), and
     * PnYnMnDTnHnMnS (combined period and duration).
     *
     * @param text the text to parse (e.g. "P1Y2M3DT4H5M6S", "PT1H30M")
     * @return the parsed period-duration
     * @throws java.time.format.DateTimeParseException if the text cannot be parsed
     */
    public static PeriodDuration valueOf(CharSequence text) {
        String input = text.toString().toUpperCase(Locale.ENGLISH);
        if (input.startsWith("PT")) {
            return of(Duration.parse(input));
        }
        int tIdx = input.indexOf('T');
        if (tIdx < 0) return new PeriodDuration(Period.parse(input), Duration.ZERO);
        return new PeriodDuration(Period.parse(input.substring(0, tIdx)), Duration.parse("P" + input.substring(tIdx)));
    }

    /**
     * Checks if this period-duration is negative.
     *
     * @return true if the period or duration is negative
     */
    public boolean isNegative() {
        return period.isNegative() || duration.isNegative();
    }

    /**
     * Checks if this period-duration is zero.
     *
     * @return true if both period and duration are zero
     */
    public boolean isZero() {
        return period.isZero() && duration.isZero();
    }

    /**
     * Returns the string representation in ISO-8601 format (e.g. P1Y2M3DT4H5M6S).
     * Returns "PT0S" for zero.
     *
     * @return the ISO-8601 string representation
     */
    @Override
    public String toString() {
        if (isZero()) {
            return "PT0S";
        }
        if (period.isZero()) {
            return duration.toString();
        }
        if (duration.isZero()) {
            return period.toString();
        }
        return period + duration.toString().substring(1);
    }
}
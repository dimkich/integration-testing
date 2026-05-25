package io.github.dimkich.integration.testing.format;

import io.github.dimkich.integration.testing.date.time.PeriodDuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PeriodDurationTest {

    private static Stream<Arguments> provideBetweenData() {
        return Stream.of(
                Arguments.of(
                        ZonedDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                        ZonedDateTime.of(2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC).minusNanos(1_000_000),
                        "P30DT23H59M59.999S"
                ),
                Arguments.of(
                        ZonedDateTime.of(2025, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                        ZonedDateTime.of(2025, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC).minusNanos(1_000_000),
                        "P27DT23H59M59.999S"
                ),
                Arguments.of(
                        ZonedDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC),
                        ZonedDateTime.of(2025, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC),
                        "P1Y1M"
                ),
                Arguments.of(
                        ZonedDateTime.of(2024, 2, 28, 0, 0, 0, 0, ZoneOffset.UTC),
                        ZonedDateTime.of(2024, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                        "P2D"
                ),
                Arguments.of(
                        ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                        ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 500_000_000, ZoneOffset.UTC),
                        "PT0.5S"
                ),
                Arguments.of(
                        ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                        ZonedDateTime.of(2026, 1, 1, 11, 0, 0, 0, ZoneOffset.UTC),
                        "PT0S"
                ),
                Arguments.of(
                        ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                        ZonedDateTime.of(2027, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                        "P1Y"
                ),
                Arguments.of(
                        Instant.ofEpochMilli(1767225600000L).atZone(ZoneOffset.UTC),
                        Instant.ofEpochMilli(1798761600000L).atZone(ZoneOffset.UTC),
                        "P1Y"
                )
        );
    }

    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("provideBetweenData")
    void testBetween(ZonedDateTime start, ZonedDateTime end, String expected) {
        PeriodDuration pd = PeriodDuration.between(start, end);
        assertEquals(expected, pd.toString());

        assertFalse(pd.getPeriod().isNegative());
        assertFalse(pd.getDuration().isNegative());
    }

    private static Stream<Arguments> provideValueOfData() {
        return Stream.of(
                Arguments.of("PT10S", Period.ZERO, Duration.ofSeconds(10)),
                Arguments.of("P1M", Period.ofMonths(1), Duration.ZERO),
                Arguments.of("P1YT1H", Period.ofYears(1), Duration.ofHours(1)),
                Arguments.of("p1y1m1dt1h1m1s", Period.of(1, 1, 1), Duration.parse("PT1H1M1S"))
        );
    }

    @ParameterizedTest
    @MethodSource("provideValueOfData")
    void testValueOf(String input, Period expectedP, Duration expectedD) {
        PeriodDuration pd = PeriodDuration.valueOf(input);
        assertEquals(expectedP, pd.getPeriod());
        assertEquals(expectedD, pd.getDuration());
    }

    @Test
    void testStatusMethods() {
        assertTrue(PeriodDuration.ZERO.isZero());
        assertFalse(PeriodDuration.ZERO.isNegative());

        PeriodDuration pos = PeriodDuration.valueOf("PT1S");
        assertFalse(pos.isZero());
        assertFalse(pos.isNegative());

        PeriodDuration neg = PeriodDuration.of(Period.ofDays(-1), Duration.ZERO);
        assertTrue(neg.isNegative());
        assertFalse(neg.isZero());
    }
}
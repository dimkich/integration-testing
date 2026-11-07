package io.github.dimkich.integration.testing.postgresql.serde;

import java.math.BigDecimal;

/**
 * Converts PostgreSQL {@code numeric}/{@code decimal} values to {@link java.math.BigDecimal};
 * special values {@code NaN}, {@code Infinity}, and {@code -Infinity} stay strings.
 */
final class PostgresBigDecimalConverter extends PostgresNumberConverter {

    @Override
    protected Object parse(String value) {
        return switch (value) {
            case "NaN", "Infinity", "-Infinity" -> value;
            default -> new BigDecimal(value);
        };
    }
}

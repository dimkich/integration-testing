package io.github.dimkich.integration.testing.postgresql.serde;

/**
 * Converts PostgreSQL {@code double precision} values to {@link Double}.
 */
final class PostgresDoubleConverter extends PostgresNumberConverter {

    @Override
    protected Double parse(String value) {
        return Double.valueOf(value);
    }
}

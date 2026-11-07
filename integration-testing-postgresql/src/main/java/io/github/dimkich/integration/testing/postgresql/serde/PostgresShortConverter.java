package io.github.dimkich.integration.testing.postgresql.serde;

/**
 * Converts PostgreSQL {@code smallint} values to {@link Short}.
 */
final class PostgresShortConverter extends PostgresNumberConverter {

    @Override
    protected Short parse(String value) {
        return Short.valueOf(value);
    }
}

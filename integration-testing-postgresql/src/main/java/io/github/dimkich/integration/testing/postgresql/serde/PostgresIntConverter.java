package io.github.dimkich.integration.testing.postgresql.serde;

/**
 * Converts PostgreSQL {@code integer} values to {@link Integer}.
 */
final class PostgresIntConverter extends PostgresNumberConverter {

    @Override
    protected Integer parse(String value) {
        return Integer.valueOf(value);
    }
}

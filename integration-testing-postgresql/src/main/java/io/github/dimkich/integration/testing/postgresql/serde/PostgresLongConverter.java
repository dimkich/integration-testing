package io.github.dimkich.integration.testing.postgresql.serde;

/**
 * Converts PostgreSQL {@code bigint} values to {@link Long}.
 */
final class PostgresLongConverter extends PostgresNumberConverter {

    @Override
    protected Long parse(String value) {
        return Long.valueOf(value);
    }
}

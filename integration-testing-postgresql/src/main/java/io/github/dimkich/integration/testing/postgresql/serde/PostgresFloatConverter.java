package io.github.dimkich.integration.testing.postgresql.serde;

/**
 * Converts PostgreSQL {@code real} values to {@link Float}.
 */
final class PostgresFloatConverter extends PostgresNumberConverter {

    @Override
    protected Float parse(String value) {
        return Float.valueOf(value);
    }
}

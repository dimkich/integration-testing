package io.github.dimkich.integration.testing.postgresql.serde;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;

import java.nio.ByteBuffer;

/**
 * Converts PostgreSQL boolean text values to {@link Boolean}; unknown text is returned as is.
 */
final class PostgresBooleanConverter extends PostgresConverter {

    @Override
    public Object convert(ByteBuffer input, PostgresColumnSerdeContext context) {
        String value = decode(input);
        if (value == null) {
            return null;
        }
        if ("t".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("f".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        return value;
    }
}

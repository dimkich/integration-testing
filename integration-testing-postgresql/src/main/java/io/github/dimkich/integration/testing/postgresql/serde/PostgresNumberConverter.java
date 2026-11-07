package io.github.dimkich.integration.testing.postgresql.serde;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;

import java.nio.ByteBuffer;

/**
 * Base converter for typed PostgreSQL numeric values.
 */
abstract class PostgresNumberConverter extends PostgresConverter {

    @Override
    public final Object convert(ByteBuffer input, PostgresColumnSerdeContext context) {
        return input == null ? null : parse(decode(input));
    }

    protected abstract Object parse(String value);
}

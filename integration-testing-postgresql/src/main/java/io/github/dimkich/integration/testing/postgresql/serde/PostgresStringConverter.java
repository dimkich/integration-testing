package io.github.dimkich.integration.testing.postgresql.serde;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;

import java.nio.ByteBuffer;

/**
 * Default converter: decodes the PostgreSQL text representation as UTF-8.
 */
final class PostgresStringConverter extends PostgresConverter {

    @Override
    public Object convert(ByteBuffer input, PostgresColumnSerdeContext context) {
        return decode(input);
    }
}

package io.github.dimkich.integration.testing.postgresql.serde;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;

/**
 * Applies the PostgreSQL {@code bytea} decoding before a custom column converter.
 */
@RequiredArgsConstructor
public final class PostgresByteaCustomConverter extends PostgresConverter {

    private static final PostgresByteaConverter BYTEA = new PostgresByteaConverter();

    private final TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> custom;

    @Override
    public Object convert(ByteBuffer input, PostgresColumnSerdeContext context) {
        byte[] bytes = BYTEA.convert(input, context);
        return bytes == null ? null : custom.convert(ByteBuffer.wrap(bytes), context);
    }

    @Override
    public Class<Object> getOutputClass() {
        return custom.getOutputClass();
    }
}

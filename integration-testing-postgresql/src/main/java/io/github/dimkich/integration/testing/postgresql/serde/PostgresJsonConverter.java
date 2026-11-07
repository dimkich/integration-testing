package io.github.dimkich.integration.testing.postgresql.serde;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;
import io.github.dimkich.integration.testing.serde.impl.JacksonDeserializer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;

/**
 * Converts PostgreSQL {@code json} and {@code jsonb} values with the configured Jackson mapper.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class PostgresJsonConverter extends PostgresConverter {

    @Getter(AccessLevel.NONE)
    private final JacksonDeserializer delegate;

    @Override
    public Object convert(ByteBuffer input, PostgresColumnSerdeContext context) {
        return delegate.convert(input, context);
    }
}

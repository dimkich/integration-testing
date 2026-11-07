package io.github.dimkich.integration.testing.postgresql.serde;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Base converter for PostgreSQL text values. It provides the common serde contract and decodes a
 * value without changing the position or limit of the caller's buffer.
 */
@Getter
abstract class PostgresConverter
        implements TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> {
    private final Class<ByteBuffer> inputClass = ByteBuffer.class;
    private final Class<Object> outputClass = Object.class;
    private final Class<PostgresColumnSerdeContext> contextClass = PostgresColumnSerdeContext.class;

    protected static String decode(ByteBuffer input) {
        if (input == null) {
            return null;
        }
        if (input.hasArray()) {
            return new String(input.array(), input.arrayOffset() + input.position(),
                    input.remaining(), StandardCharsets.UTF_8);
        }
        return StandardCharsets.UTF_8.decode(input.slice()).toString();
    }
}

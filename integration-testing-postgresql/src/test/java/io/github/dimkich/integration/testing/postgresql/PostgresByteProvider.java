package io.github.dimkich.integration.testing.postgresql;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;
import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProviderFactory;
import org.springframework.lang.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Test-only provider that demonstrates an explicit {@link Byte} serde override for a PostgreSQL
 * column.
 */
public final class PostgresByteProvider implements TestSerdeProviderFactory<ByteBuffer, Object,
        PostgresColumnSerdeContext, ComponentRole, StandardSerdeProperties> {

    @Override
    public String getName() {
        return "it-byte";
    }

    @Override
    public Class<ByteBuffer> getInputClass() {
        return ByteBuffer.class;
    }

    @Override
    public Class<Object> getOutputClass() {
        return Object.class;
    }

    @Override
    public Class<PostgresColumnSerdeContext> getContextClass() {
        return PostgresColumnSerdeContext.class;
    }

    @Override
    public Class<StandardSerdeProperties> getPropertiesClass() {
        return StandardSerdeProperties.class;
    }

    @Override
    public TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> create(
            StandardSerdeProperties config, Class<ByteBuffer> inputClass, Class<Object> outputClass,
            Class<PostgresColumnSerdeContext> contextClass, @Nullable ComponentRole role) {
        return TestSerdeConverter.of(ByteBuffer.class, Object.class, PostgresColumnSerdeContext.class,
                (input, context) -> Byte.valueOf(
                        StandardCharsets.UTF_8.decode(input.duplicate()).toString().trim()));
    }
}

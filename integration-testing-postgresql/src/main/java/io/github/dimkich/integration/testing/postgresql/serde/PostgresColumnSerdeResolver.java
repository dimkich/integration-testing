package io.github.dimkich.integration.testing.postgresql.serde;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;
import io.github.dimkich.integration.testing.postgresql.config.PostgresColumnProperties;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTypeDescriptor;
import io.github.dimkich.integration.testing.serde.SerdeManager;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;

/**
 * Resolves the converter of one PostgreSQL column. A column with serde configuration gets a custom
 * converter; the built-in type converter is used only when no override is configured. The custom
 * converter is authoritative for every non-null value of its column.
 */
@RequiredArgsConstructor
public class PostgresColumnSerdeResolver {
    private final SerdeManager serdeManager;

    /**
     * Resolves the converter for one column. A configured serde override is authoritative and is
     * returned without a built-in fallback; for {@code bytea}, the PostgreSQL binary decoding is
     * applied before the custom converter. Columns without a serde override use the converter
     * selected from the PostgreSQL type.
     *
     * @param type PostgreSQL type descriptor
     * @param configured column configuration, or {@code null}
     * @return resolved converter
     */
    public TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> resolve(
            PostgresTypeDescriptor type, @Nullable PostgresColumnProperties configured) {
        if (configured == null || !configured.hasSerdeConfiguration()) {
            return defaultConverter(type);
        }
        TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> custom =
                serdeManager.resolve(configured, ByteBuffer.class, Object.class,
                        PostgresColumnSerdeContext.class, type);
        if (type.isBytea()) {
            custom = new PostgresByteaCustomConverter(custom);
        }
        return custom;
    }

    private TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> defaultConverter(
            PostgresTypeDescriptor type) {
        return serdeManager.resolve(TestSerdeProperties.Default.INSTANCE, ByteBuffer.class,
                Object.class, PostgresColumnSerdeContext.class, type);
    }
}

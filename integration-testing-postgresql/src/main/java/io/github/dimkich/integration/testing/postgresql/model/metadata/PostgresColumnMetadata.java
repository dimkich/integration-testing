package io.github.dimkich.integration.testing.postgresql.model.metadata;

import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

import java.nio.ByteBuffer;

/**
 * Metadata for one PostgreSQL column. It is stored inside {@link PostgresTableMetadata}, which in
 * turn is attached to the canonical table object.
 */
@Value
public class PostgresColumnMetadata {
    int index;
    String name;
    @Getter(AccessLevel.NONE)
    TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> converter;
    boolean generated;
    boolean identityAlways;
    /**
     * Whether the whole column is excluded from the storage difference. The flag is applied by
     * the difference engine; audit and Smart Revert still use every column.
     */
    boolean ignore;
    /**
     * Serde context cached for the whole lifetime of the prepared column metadata.
     */
    @Getter(AccessLevel.NONE)
    PostgresColumnSerdeContext context;

    /**
     * Converts one raw column value through the configured converter.
     *
     * @param value raw field bytes, or {@code null} for SQL {@code NULL}
     * @return the converted value, or {@code null}
     * @throws IllegalArgumentException when the configured converter fails; the message includes
     *                                  the table, column, and PostgreSQL type
     */
    public Object convert(ByteBuffer value) {
        if (value == null) {
            return null;
        }
        // Converters may rethrow checked exceptions through @SneakyThrows.
        try {
            return converter.convert(value, context);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert PostgreSQL column "
                    + context.getTableName() + "." + context.getColumnName() + " of type "
                    + context.getSqlTypeName(), e);
        }
    }
}

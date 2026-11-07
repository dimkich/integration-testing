package io.github.dimkich.integration.testing.postgresql.model.metadata;

import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Serde context of one PostgreSQL column. The canonical {@link Table} is the source of the table
 * name and metadata; the column descriptor carries the PostgreSQL type and converter information.
 * Nested composite fields keep the parent table/column and only replace the type descriptor.
 * <p>
 * The context is created once per column metadata. Contexts of nested composite fields and array
 * elements are built eagerly, so value conversion does not allocate them per value.
 * Instances are created by {@link #of} so that nested contexts are initialized consistently.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PostgresColumnSerdeContext implements TestSerdeContext {
    private final Table table;
    private final String columnName;
    private final PostgresTypeDescriptor type;
    private final DateTimeService dateTimeService;
    private final PostgresColumnSerdeContext elementContext;
    @Getter(AccessLevel.NONE)
    private final List<PostgresColumnSerdeContext> fieldContexts;
    private final String sqlTypeNameLower;

    /**
     * Creates the context for a column and eagerly builds nested element and composite-field
     * contexts used during conversion.
     *
     * @param table canonical table
     * @param columnName column name
     * @param type PostgreSQL type descriptor
     * @param dateTimeService date/time service used for timezone-aware values
     * @return a context for the column
     */
    public static PostgresColumnSerdeContext of(Table table, String columnName,
                                           PostgresTypeDescriptor type,
                                           DateTimeService dateTimeService) {
        return new PostgresColumnSerdeContext(
                table,
                columnName,
                type,
                dateTimeService,
                createElementContext(table, columnName, type, dateTimeService),
                createFieldContexts(table, columnName, type, dateTimeService),
                type.getName().toLowerCase(Locale.ROOT));
    }

    private static PostgresColumnSerdeContext createElementContext(Table table, String columnName,
                                                              PostgresTypeDescriptor type,
                                                              DateTimeService dateTimeService) {
        return type.isArray()
                ? of(table, columnName, type.getElement(), dateTimeService)
                : null;
    }

    private static List<PostgresColumnSerdeContext> createFieldContexts(Table table, String columnName,
                                                                   PostgresTypeDescriptor type,
                                                                   DateTimeService dateTimeService) {
        if (type.getFields().isEmpty()) {
            return List.of();
        }
        List<PostgresColumnSerdeContext> contexts = new ArrayList<>(type.getFields().size());
        for (PostgresCompositeField field : type.getFields()) {
            contexts.add(of(table, columnName, field.getType(), dateTimeService));
        }
        return contexts;
    }

    /**
     * Canonical table name, including the schema.
     *
     * @return canonical table name
     */
    public String getTableName() {
        return table.getCanonicalName();
    }

    /**
     * PostgreSQL type name used by type-specific converters.
     *
     * @return SQL type name
     */
    public String getSqlTypeName() {
        return type.getName();
    }

    /**
     * Zone for timezone-aware temporal values: the mocked test time zone or UTC.
     *
     * @return zone for temporal conversion
     */
    public ZoneId getZoneId() {
        ZonedDateTime dateTime = dateTimeService.getDateTime();
        return dateTime != null ? dateTime.getZone() : ZoneOffset.UTC;
    }

    /**
     * Context of a composite field by declaration order.
     *
     * @param index field index
     * @return field context
     */
    public PostgresColumnSerdeContext getFieldContext(int index) {
        return fieldContexts.get(index);
    }
}

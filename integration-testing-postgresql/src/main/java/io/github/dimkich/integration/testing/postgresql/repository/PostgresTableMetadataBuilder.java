package io.github.dimkich.integration.testing.postgresql.repository;

import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.postgresql.config.PostgresColumnProperties;
import io.github.dimkich.integration.testing.postgresql.config.PostgresProperties;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnMetadata;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTableMetadata;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTypeDescriptor;
import io.github.dimkich.integration.testing.postgresql.serde.PostgresColumnSerdeResolver;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionProcessor;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Accumulates the metadata of one table while the repository walks the catalog result set.
 */
@RequiredArgsConstructor
class PostgresTableMetadataBuilder {
    private final Table table;
    private final PostgresProperties.Connection connectionProperties;
    private final PostgresColumnSerdeResolver serdeResolver;
    private final DateTimeService dateTimeService;

    private final List<PostgresColumnMetadata> columns = new ArrayList<>();
    private final SortedMap<Integer, String> primaryKeys = new TreeMap<>();
    private final Set<String> exclusionPaths = new LinkedHashSet<>();
    private final Set<String> excludedColumns = new LinkedHashSet<>();
    private List<String> sequences;

    /**
     * Adds one column read from the current metadata row.
     */
    String addColumn(ResultSet rs, PostgresTypeDescriptor type) throws SQLException {
        String columnName = rs.getString("attname");
        PostgresColumnProperties configured = connectionProperties.resolveColumn(table, columnName);
        boolean ignore = configured != null && configured.isIgnore();
        PostgresColumnSerdeContext context = PostgresColumnSerdeContext.of(table, columnName, type, dateTimeService);
        TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> converter = serdeResolver.resolve(type, configured);
        columns.add(new PostgresColumnMetadata(columns.size(), columnName, converter,
                rs.getBoolean("generated"), rs.getBoolean("identity_always"), ignore, context));
        addExclusions(columnName, configured);
        return columnName;
    }

    void addPrimaryKey(int ordinal, String columnName) {
        primaryKeys.put(ordinal, columnName);
    }

    void setSequences(List<String> sequences) {
        if (this.sequences == null && sequences != null) {
            this.sequences = sequences;
        }
    }

    PostgresTableMetadata build(FieldExclusionProcessor processor) {
        List<String> primaryKey = List.copyOf(primaryKeys.values());
        validatePrimaryKeyExclusions(primaryKey);
        return new PostgresTableMetadata(table, columns, primaryKey,
                sequences == null ? List.of() : sequences,
                processor.compile(exclusionPaths),
                connectionProperties.getSequenceRestartValue());
    }

    private void addExclusions(String columnName, PostgresColumnProperties configured) {
        if (configured == null || configured.isIgnore() || configured.getExcludedFields() == null) {
            return;
        }
        boolean added = false;
        for (String path : configured.getExcludedFields()) {
            if (path != null && !path.isBlank()) {
                exclusionPaths.add(columnName + "." + path);
                added = true;
            }
        }
        if (added) {
            excludedColumns.add(columnName);
        }
    }

    private void validatePrimaryKeyExclusions(List<String> primaryKey) {
        if (primaryKey.isEmpty() || excludedColumns.isEmpty()) {
            return;
        }
        Set<String> primaryKeyColumns = new HashSet<>(primaryKey);
        for (String columnName : excludedColumns) {
            if (primaryKeyColumns.contains(columnName)) {
                throw new IllegalArgumentException("Primary key column '" + columnName
                        + "' of table " + table.getCanonicalName()
                        + " cannot have excluded fields in the storage difference");
            }
        }
    }
}

package io.github.dimkich.integration.testing.postgresql.model.metadata;

import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionTree;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableMetadata;
import io.github.dimkich.integration.testing.storage.sql.table.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Metadata for a PostgreSQL table cached on the canonical
 * {@link io.github.dimkich.integration.testing.storage.sql.table.Table}. It is the single source
 * of table, column, primary-key, sequence and field-exclusion facts for audit parsing, the
 * difference engine and the Smart Revert.
 * <p>
 * Derived collections and column indexes are built once so hot paths do not rescan the column
 * list or allocate temporary sets per row.
 */
@Getter
@ToString(of = {"table", "columns", "primaryKey", "sequenceRestartSql"})
public final class PostgresTableMetadata implements TableMetadata {
    private final Table table;
    private final List<PostgresColumnMetadata> columns;
    @Getter(AccessLevel.NONE)
    private final List<PostgresColumnMetadata> visibleColumns;
    private final List<String> primaryKey;
    /**
     * Compiled nested-value exclusions. Whole-column ignores are handled before conversion.
     */
    @Getter(AccessLevel.NONE)
    private final FieldExclusionTree excludedFields;
    @Getter(AccessLevel.NONE)
    private final String primaryKeyMatchSql;
    @Getter(AccessLevel.NONE)
    private final String deleteMatchSql;
    /**
     * DDL restarting the sequences owned by this table.
     */
    private final List<String> sequenceRestartSql;

    @Getter
    private final int[] primaryKeyIndexes;
    @Getter
    private final int[] identityAlwaysIndexes;
    private final Set<String> identityAlways;

    /**
     * Whether a plain UPDATE revert has at least one writable column.
     */
    @Getter(AccessLevel.NONE)
    private final boolean hasUpdateColumns;
    /**
     * Pre-rendered column fragments shared by all revert statements of the table.
     */
    private final String insertColumnsSql;
    private final String insertSelectSql;
    private final String updateAssignmentsSql;

    public PostgresTableMetadata(Table table, List<PostgresColumnMetadata> columns,
                                 List<String> primaryKey, List<String> sequences,
                                 FieldExclusionTree excludedFields, long sequenceRestartValue) {
        this.table = table;
        this.columns = columns;
        this.primaryKey = primaryKey;
        this.excludedFields = excludedFields;
        validatePrimaryKeyIgnored(primaryKey);

        Map<String, PostgresColumnMetadata> byName = new HashMap<>(this.columns.size());
        List<String> writable = new ArrayList<>();
        List<PostgresColumnMetadata> visible = new ArrayList<>(this.columns.size());
        Set<String> always = new LinkedHashSet<>();
        Set<String> immutable = new HashSet<>();
        for (PostgresColumnMetadata column : this.columns) {
            byName.putIfAbsent(column.getName(), column);
            if (!column.isIgnore()) {
                visible.add(column);
            }
            if (column.isGenerated()) {
                immutable.add(column.getName());
            } else {
                writable.add(column.getName());
            }
            if (column.isIdentityAlways()) {
                always.add(column.getName());
                immutable.add(column.getName());
            }
        }
        this.visibleColumns = visible;
        this.identityAlways = always;

        Set<String> primary = new HashSet<>(this.primaryKey);
        List<String> update = new ArrayList<>();
        for (String column : writable) {
            if (!primary.contains(column) && !immutable.contains(column)) {
                update.add(column);
            }
        }
        this.hasUpdateColumns = !update.isEmpty();
        this.insertColumnsSql = writable.stream()
                .map(TableName::quote)
                .collect(Collectors.joining(", "));
        this.insertSelectSql = writable.stream()
                .map(column -> "(v.r)." + TableName.quote(column))
                .collect(Collectors.joining(", "));
        this.updateAssignmentsSql = update.stream()
                .map(column -> TableName.quote(column) + " = (v.r)." + TableName.quote(column))
                .collect(Collectors.joining(", "));

        this.primaryKeyIndexes = indexes(byName, this.primaryKey, table);
        this.identityAlwaysIndexes = indexes(byName, this.identityAlways, table);
        this.primaryKeyMatchSql = buildPrimaryKeyMatch(this.primaryKey);
        this.deleteMatchSql = this.primaryKeyMatchSql == null
                ? "to_jsonb(t) = to_jsonb(v.r)" : this.primaryKeyMatchSql;
        this.sequenceRestartSql = buildSequenceRestartSql(sequences, sequenceRestartValue);
    }

    /**
     * Converts both tuple versions into diff-visible row values. Ignored columns are skipped
     * before conversion; nested value exclusions are applied to the converted maps.
     */
    public PostgresRowValues convert(ByteBuffer[] oldFields, ByteBuffer[] newFields) {
        int visibleCount = visibleColumns.size();
        Map<String, Object> oldResult = oldFields == null
                ? null : new LinkedHashMap<>(visibleCount);
        Map<String, Object> newResult = newFields == null
                ? null : new LinkedHashMap<>(visibleCount);
        for (PostgresColumnMetadata column : visibleColumns) {
            int index = column.getIndex();
            ByteBuffer oldField = oldFields == null ? null : oldFields[index];
            ByteBuffer newField = newFields == null ? null : newFields[index];
            if (oldResult != null && newResult != null && Objects.equals(oldField, newField)) {
                Object value = column.convert(oldField);
                oldResult.put(column.getName(), value);
                newResult.put(column.getName(), value);
                continue;
            }
            if (oldResult != null) {
                oldResult.put(column.getName(), column.convert(oldField));
            }
            if (newResult != null) {
                newResult.put(column.getName(), column.convert(newField));
            }
        }
        if (oldResult != null) {
            excludedFields.process(oldResult);
        }
        if (newResult != null) {
            excludedFields.process(newResult);
        }
        return new PostgresRowValues(oldResult, newResult);
    }

    public boolean hasUpdateColumns() {
        return hasUpdateColumns;
    }

    /**
     * SQL condition matching a target table alias against a VALUES row alias by primary key.
     */
    public String primaryKeyMatch() {
        if (primaryKeyMatchSql == null) {
            throw new IllegalStateException("Table has no primary key");
        }
        return primaryKeyMatchSql;
    }

    /**
     * SQL condition for deleting a row: by primary key when available, otherwise by the complete
     * row value.
     */
    public String deleteMatch() {
        return deleteMatchSql;
    }

    private void validatePrimaryKeyIgnored(List<String> primaryKey) {
        if (primaryKey.isEmpty()) {
            return;
        }
        Set<String> primaryKeyColumns = new HashSet<>(primaryKey);
        for (PostgresColumnMetadata column : columns) {
            if (column.isIgnore() && primaryKeyColumns.contains(column.getName())) {
                throw new IllegalArgumentException("Primary key column '" + column.getName()
                        + "' of table " + table.getCanonicalName()
                        + " cannot be ignored in the storage difference");
            }
        }
    }

    private static String buildPrimaryKeyMatch(List<String> primaryKey) {
        if (primaryKey.isEmpty()) {
            return null;
        }
        return primaryKey.stream()
                .map(column -> "t." + TableName.quote(column) + " = (v.r)." + TableName.quote(column))
                .collect(Collectors.joining(" AND "));
    }

    private static List<String> buildSequenceRestartSql(List<String> sequences,
                                                        long sequenceRestartValue) {
        List<String> sql = new ArrayList<>(sequences.size());
        for (String sequence : sequences) {
            sql.add("ALTER SEQUENCE " + sequence + " RESTART WITH " + sequenceRestartValue);
        }
        return sql;
    }

    private static int[] indexes(Map<String, PostgresColumnMetadata> columnsByName,
                                 Collection<String> names, Table table) {
        int[] result = new int[names.size()];
        int index = 0;
        for (String name : names) {
            PostgresColumnMetadata column = columnsByName.get(name);
            if (column == null) {
                throw new IllegalStateException("Unknown column '" + name
                        + "' for table metadata " + table);
            }
            result[index++] = column.getIndex();
        }
        return result;
    }
}

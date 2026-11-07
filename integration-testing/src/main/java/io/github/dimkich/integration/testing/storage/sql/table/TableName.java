package io.github.dimkich.integration.testing.storage.sql.table;

import lombok.Data;
import org.springframework.lang.NonNull;

import java.util.Objects;

/**
 * Qualified name of a database table. The schema may be unknown until the storage metadata is
 * read; in that case {@link #getSchema()} is {@code null}.
 */
@Data
public final class TableName implements Comparable<TableName> {
    private final String schema;
    private final String table;

    public TableName(String schema, String table) {
        this.table = Objects.requireNonNull(table, "table").trim();
        this.schema = schema == null || schema.isBlank() ? null : schema.trim();
        if (this.table.isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be empty");
        }
    }

    public static TableName unqualified(String table) {
        return new TableName(null, table);
    }

    public static TableName of(String schema, String table) {
        return new TableName(schema, table);
    }

    public static TableName parse(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Table name cannot be empty");
        }
        String value = name.trim();
        int dot = value.lastIndexOf('.');
        if (dot < 0) {
            return new TableName(null, value);
        }
        return new TableName(value.substring(0, dot), value.substring(dot + 1));
    }

    public String getUnqualified() {
        return table;
    }

    /**
     * Returns this name with the default schema applied when the schema is not set yet.
     */
    public TableName qualify(String defaultSchema) {
        if (schema != null) {
            return this;
        }
        return new TableName(Objects.requireNonNull(defaultSchema, "defaultSchema"), table);
    }

    /**
     * SQL representation. Identifiers are quoted so case and reserved words are preserved.
     */
    public String getQualified() {
        if (schema == null) {
            return quote(table);
        }
        return quote(schema) + "." + quote(table);
    }

    /**
     * Human-readable representation used by logs and messages.
     */
    public String getCanonicalName() {
        return schema == null ? table : schema + "." + table;
    }

    public static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String toString() {
        return getCanonicalName();
    }

    @Override
    public int compareTo(@NonNull TableName other) {
        if (schema == null) {
            if (other.schema != null) {
                return -1;
            }
        } else if (other.schema == null) {
            return 1;
        } else {
            int schemaComparison = schema.compareTo(other.schema);
            if (schemaComparison != 0) {
                return schemaComparison;
            }
        }
        return table.compareTo(other.table);
    }
}

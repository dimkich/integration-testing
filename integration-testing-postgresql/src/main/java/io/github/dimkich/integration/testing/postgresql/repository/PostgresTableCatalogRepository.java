package io.github.dimkich.integration.testing.postgresql.repository;

import io.github.dimkich.integration.testing.postgresql.PostgresDatabase;
import io.github.dimkich.integration.testing.storage.sql.table.TableName;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Discovers the PostgreSQL tables available to one test storage. The discovered set is cached
 * until {@link #reset()}.
 */
@RequiredArgsConstructor
public class PostgresTableCatalogRepository {
    private final PostgresDatabase database;

    private Set<TableName> tables;

    /**
     * Tables discovered for this storage. The returned set is the cached live view owned by this
     * repository and must not be mutated by callers; {@link #reset()} drops it.
     */
    public Set<TableName> getTables() throws SQLException {
        if (tables != null) {
            return tables;
        }
        String sql = """
                SELECT n.nspname AS schema_name, c.relname AS table_name
                FROM pg_class c
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE %s
                ORDER BY n.nspname, c.relname
                """.formatted(PostgresCatalogSql.TABLE_FILTER);
        List<TableName> discovered = database.query(sql,
                statement -> PostgresCatalogSql.bindTableFilter(statement, 1),
                resultSet -> TableName.of(resultSet.getString("schema_name"),
                        resultSet.getString("table_name")));
        tables = new LinkedHashSet<>(discovered);
        return tables;
    }

    public void reset() {
        tables = null;
    }
}

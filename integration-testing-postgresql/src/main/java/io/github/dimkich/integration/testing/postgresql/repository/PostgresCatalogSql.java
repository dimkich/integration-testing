package io.github.dimkich.integration.testing.postgresql.repository;

import lombok.experimental.UtilityClass;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Shared PostgreSQL catalog fragments. Both the table catalog and the metadata repository load
 * application tables, so the internal-table filter and its parameters must stay in sync.
 */
@UtilityClass
class PostgresCatalogSql {
    /**
     * Application-table filter for a query that aliases {@code pg_class} as {@code c} and
     * {@code pg_namespace} as {@code n}. It contains two placeholders for the internal table names.
     */
    static final String TABLE_FILTER = """
            c.relkind IN ('r', 'p')
              AND n.nspname NOT IN ('pg_catalog', 'information_schema')
              AND c.relname <> ?
              AND strpos(c.relname::text, ?::text) <> 1""";

    static int bindTableFilter(PreparedStatement statement, int parameter) throws SQLException {
        statement.setString(parameter++, PostgresInternalTableNames.LEGACY_AUDIT_LOG);
        statement.setString(parameter++, PostgresInternalTableNames.AUDIT_JOURNAL_PREFIX);
        return parameter;
    }
}

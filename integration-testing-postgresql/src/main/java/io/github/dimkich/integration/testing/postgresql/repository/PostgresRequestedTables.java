package io.github.dimkich.integration.testing.postgresql.repository;

import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Requested tables of one catalog query. It owns the SQL predicate and the prepared statement
 * parameters, so the catalog repositories only execute the query and map rows.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class PostgresRequestedTables {
    private static final String SCHEMA_ALIAS = "n";
    private static final String TABLE_ALIAS = "c";

    private final TableSet tables;

    static PostgresRequestedTables of(TableSet tables) {
        return new PostgresRequestedTables(tables);
    }

    String predicate() {
        StringBuilder result = new StringBuilder(Math.max(16, tables.size() * 48));
        for (Table table : tables) {
            if (!result.isEmpty()) {
                result.append(" OR ");
            }
            result.append('(').append(SCHEMA_ALIAS).append(".nspname = ? AND ")
                    .append(TABLE_ALIAS).append(".relname = ?)");
        }
        return result.toString();
    }

    void bind(PreparedStatement statement, int parameter) throws SQLException {
        int index = parameter;
        for (Table table : tables) {
            statement.setString(index++, table.getSchema());
            statement.setString(index++, table.getTable());
        }
    }
}

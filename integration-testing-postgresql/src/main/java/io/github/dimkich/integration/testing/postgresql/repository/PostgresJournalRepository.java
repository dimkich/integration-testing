package io.github.dimkich.integration.testing.postgresql.repository;

import io.github.dimkich.integration.testing.postgresql.PostgresDatabase;
import io.github.dimkich.integration.testing.postgresql.model.change.PostgresJournalAction;
import io.github.dimkich.integration.testing.postgresql.model.change.PostgresJournalRecord;
import io.github.dimkich.integration.testing.postgresql.model.change.PostgresRawRow;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableName;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Rich repository of the PostgreSQL audit journal. It owns the journal schema/triggers, reads the
 * raw rows and maps them to diff-ready {@link PostgresJournalRecord}s.
 */
@RequiredArgsConstructor
public class PostgresJournalRepository {
    private final PostgresDatabase database;

    public void initialize() throws SQLException {
        database.execute(PostgresJournalSql.initializationSql());
    }

    public void attachTriggers(TableSet requestedTables) throws SQLException {
        database.execute(PostgresJournalSql.attachTriggersSql(requestedTables));
    }

    public void truncate() throws SQLException {
        database.execute(PostgresJournalSql.truncateSql());
    }

    public List<PostgresJournalRecord> readAfter(long lastReadId, TableRegistry registry,
                                                 @Nullable String postReadSql) throws SQLException {
        String select = PostgresJournalSql.selectAfter(lastReadId);
        String sql = postReadSql == null || postReadSql.isBlank()
                ? select
                : select + ";\n" + postReadSql;
        return database.query(sql, resultSet -> readRecord(resultSet, registry));
    }

    private PostgresJournalRecord readRecord(ResultSet rs, TableRegistry registry) throws SQLException {
        TableName tableName = TableName.parse(rs.getString("table_name"));
        Table table = registry.resolveOrRegister(tableName);
        int nodeValue = rs.getInt("node");
        Integer node = rs.wasNull() ? null : nodeValue;
        PostgresRawRow oldRow = toRawRow(table, rs.getBytes("old_data"));
        PostgresRawRow newRow = toRawRow(table, rs.getBytes("new_data"));
        return new PostgresJournalRecord(
                rs.getLong("id"),
                table,
                PostgresJournalAction.fromCode(rs.getString("action")),
                oldRow,
                newRow,
                node);
    }

    private static PostgresRawRow toRawRow(Table table, byte[] data) {
        return data == null ? null : new PostgresRawRow(table, ByteBuffer.wrap(data));
    }
}

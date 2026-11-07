package io.github.dimkich.integration.testing.postgresql;

import io.github.dimkich.integration.testing.postgresql.repository.PostgresInternalTableNames;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import io.github.dimkich.integration.testing.storage.sql.table.TableName;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Application-side facade used by the test XML: executes raw SQL through the application
 * {@link javax.sql.DataSource} (the changes are captured by the database triggers) and reads values
 * back to prove that the Smart Revert restored the initial state.
 */
@SuppressWarnings("SqlSourceToSinkFlow")
@RequiredArgsConstructor
public class PostgresFacade {
    private final JdbcTemplate jdbcTemplate;
    private final SQLDataStorageService dataStorageService;

    public void execute(String sql) {
        jdbcTemplate.execute(sql);
    }

    /**
     * Executes SQL and returns the message of the failure raised while building the storage diff.
     * Used by negative tests that assert conversion errors without failing the whole test.
     */
    public String executeAndGetDiffError(String sql) {
        jdbcTemplate.execute(sql);
        try {
            dataStorageService.getDiff();
            return "no error";
        } catch (Exception e) {
            return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
    }

    @Transactional
    public void executeAndRollback(String sql) {
        jdbcTemplate.execute(sql);
        throw new RuntimeException("Forced rollback");
    }

    /**
     * Reads one scalar expression from a table row selected by {@code id}.
     */
    public Object selectValue(String table, String expression, Object id) {
        return jdbcTemplate.queryForObject(
                "select " + expression + " from " + table + " where id = ?", Object.class, id);
    }

    public Integer countRows(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    /**
     * Reads one scalar value from an arbitrary query.
     */
    public Object selectScalar(String sql) {
        return jdbcTemplate.queryForObject(sql, Object.class);
    }

    public void setStandardConformingStrings(boolean enabled) throws Exception {
        dataStorageService.executeSqls(
                List.of("SET standard_conforming_strings = " + (enabled ? "on" : "off")));
    }

    public Integer getTruncateJournalCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + PostgresInternalTableNames.AUDIT_JOURNAL
                        + " where action = 'T'", Integer.class);
    }

    /**
     * Re-reads the table metadata and formats any failure, so a test can assert schema discovery
     * without depending on a particular exception type.
     */
    public String getTablesError() {
        try {
            dataStorageService.init();
            return dataStorageService.getTableRegistry().find(TableName.parse("other.users")) != null
                    ? "no error" : "table other.users is not registered";
        } catch (Exception e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}

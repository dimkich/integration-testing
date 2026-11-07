package io.github.dimkich.integration.testing.storage.sql;

import io.github.dimkich.integration.testing.dbunit.DbUnitFixture;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;

import java.util.Collection;

/**
 * Abstraction over a SQL data storage used by integration tests.
 */
public interface SQLDataStorage {

    String getName();

    /**
     * Drops cached schema and audit state (journal, watermark and tagged SQL), reloads the table
     * catalog and prepares metadata for the current tables. It is called after an init setup, so
     * implementations can discover the schema and attach complete table, column, key and sequence
     * information to the canonical
     * {@link io.github.dimkich.integration.testing.storage.sql.table.Table} objects.
     */
    void initialize() throws Exception;

    /**
     * Executes a batch of SQL. {@code untrackedSql} is infrastructure SQL (setup, rollback,
     * clearing) and is executed with triggers disabled. {@code trackedSql} is the target SQL
     * executed normally; the storage keeps its correlation tags until
     * {@link #getStorageChanges(String)} resolves the applied mutations.
     *
     * @param untrackedSql SQL that must not appear in the audit diff
     * @param trackedSql the target SQL statements, in execution order; identical texts are allowed
     * @throws Exception any error during SQL execution
     */
    void executeSql(Collection<String> untrackedSql, Collection<String> trackedSql) throws Exception;

    /**
     * Loads the requested tables from the fixture into the storage. The storage owns the concrete
     * DBUnit operation and connection settings (for example, a clean insert for PostgreSQL); the
     * fixture only exposes the already parsed data set.
     */
    void loadFixture(DbUnitFixture fixture, TableSet tables) throws Exception;

    /**
     * SQL that removes all rows from the given tables without producing audit changes.
     */
    String getClearSql(TableSet tables);

    /**
     * SQL that restarts the sequences of the managed tables, or an empty string when the storage
     * has no sequences to reset. Restarting to the base value keeps the identifiers of dynamic
     * data deterministic: fixtures use explicit ids, so a rolled back or reverted mutation cannot
     * leave a sequence drifted past them.
     */
    String getSequenceRestartSql();

    /**
     * Reads the tracked changes newer than the previous read exactly once. The returned result
     * builds only the requested part and does not mutate the storage when its accessors are called.
     *
     * @param postReadSql SQL to execute after the audit select, or {@code null}
     * @return the lazy result of the read
     * @throws Exception if an error occurs while reading
     */
    StorageChanges getStorageChanges(String postReadSql) throws Exception;
}

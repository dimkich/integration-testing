package io.github.dimkich.integration.testing.storage.sql;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.dataset.IDataSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Abstraction over a SQL data storage used by integration tests.
 * <p>
 * Implementations are responsible for executing SQL, loading DBUnit datasets,
 * and generating vendor-specific SQL snippets to control and clean test data.
 */
public interface SQLDataStorage {

    /**
     * Returns the logical name of this storage (for example, a data source alias).
     *
     * @return storage name
     */
    String getName();

    /**
     * Executes the given SQL statements in the context of this storage.
     *
     * @param sql collection of SQL statements to execute
     * @throws Exception any error during SQL execution
     */
    void executeSql(Collection<String> sql) throws Exception;

    /**
     * Reads data from the specified tables and returns them in a storage-specific format.
     *
     * @param tables       tables to read
     * @param excludedRows map of table name to set of row identifiers (for example, primary keys)
     *                     that must be excluded from the result
     * @return a map containing table data; concrete structure depends on implementation
     * @throws Exception any error while reading data
     */
    Map<String, Object> getTablesData(Collection<String> tables,
                                      Map<String, Set<String>> excludedRows) throws Exception;

    /**
     * Loads the given DBUnit dataset into the storage.
     *
     * @param dataSet dataset to load
     * @throws Exception any error while loading data
     */
    void loadDataset(IDataSet dataSet) throws Exception;

    /**
     * Provides DBUnit configuration for this storage.
     *
     * @return DBUnit {@link DatabaseConfig}
     * @throws Exception any error while building configuration
     */
    DatabaseConfig getDbunitConfig() throws Exception;

    /**
     * Returns the list of tables that are visible in this storage.
     *
     * @return set of table names
     * @throws Exception any error while retrieving metadata
     */
    Set<String> getTables() throws Exception;

    /**
     * Initializes table restriction for the given tables.
     * <p>
     * Implementations can use this method to restrict visibility or operations
     * to the specified subset of tables.
     *
     * @param tables tables that should be restricted
     * @throws Exception any error while applying restrictions
     */
    void initTablesRestriction(Collection<String> tables) throws Exception;

    /**
     * Builds SQL statement that removes restrictions and allows operations for the given table.
     *
     * @param table table name
     * @return SQL statement that allows the table
     */
    String getAllowTableSql(String table);

    /**
     * Builds SQL statement that restricts operations for the given table.
     *
     * @param table table name
     * @return SQL statement that restricts the table
     */
    String getRestrictTableSql(String table);

    /**
     * Builds SQL statement that clears data from the given tables.
     *
     * @param tables tables to clear
     * @return SQL statement that deletes or truncates data in the tables
     */
    String getClearSql(Collection<String> tables);

    /**
     * Builds SQL statement that restarts identity/sequence values for the given tables.
     *
     * @param tables tables whose identity values must be restarted
     * @return SQL statement that restarts identities
     */
    String getRestartIdentitySql(Collection<String> tables);
}

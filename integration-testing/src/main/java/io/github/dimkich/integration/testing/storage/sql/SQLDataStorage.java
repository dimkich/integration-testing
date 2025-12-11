package io.github.dimkich.integration.testing.storage.sql;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.dataset.IDataSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface SQLDataStorage {
    String getName();

    void executeSql(Collection<String> sql) throws Exception;

    Map<String, Object> getTablesData(Collection<String> tables, Map<String, Set<String>> excludedRows) throws Exception;

    void loadDataset(IDataSet dataSet) throws Exception;

    DatabaseConfig getDbunitConfig() throws Exception;

    Set<String> getTables() throws Exception;

    void initTablesRestriction(Collection<String> tables) throws Exception;

    String getAllowTableSql(String table);

    String getRestrictTableSql(String table);

    String getClearSql(Collection<String> tables);

    String getRestartIdentitySql(Collection<String> tables);
}

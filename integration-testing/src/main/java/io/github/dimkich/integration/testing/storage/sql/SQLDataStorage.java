package io.github.dimkich.integration.testing.storage.sql;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.dataset.IDataSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface SQLDataStorage {
    String getName();

    void executeSql(Collection<String> sql) throws Exception;

    Map<String, Map<String, Object>> getTablesData(Collection<String> tables) throws Exception;

    void loadDataset(IDataSet dataSet) throws Exception;

    DatabaseConfig getDbunitConfig() throws Exception;

    Set<String> initTableRestriction() throws Exception;

    TableRestrictionBuilder getRestrictionBuilder();

    void setTablesToClear(Collection<String> tablesToClear);

    void clearTables() throws Exception;
}

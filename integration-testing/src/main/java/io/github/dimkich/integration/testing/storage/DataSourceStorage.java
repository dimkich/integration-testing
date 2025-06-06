package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.TestDataStorage;
import lombok.Cleanup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.sql.Date;
import java.sql.*;
import java.util.*;

@RequiredArgsConstructor
public class DataSourceStorage implements TestDataStorage {
    private final String name;
    @Getter
    private final Connection connection;
    private final TablesRestrictionService tablesRestrictionService;

    private Set<String> tableNames = Set.of();
    private Map<String, List<String>> primaryKeys;
    private String clearSql;

    @Override
    public String getName() {
        return name;
    }

    public void setTableNames(Set<String> tableNames) throws SQLException {
        tablesRestrictionService.setAllowedTables(tableNames);
        if (tableNames == null || tableNames.isEmpty()) {
            clearSql = null;
            this.tableNames = Set.of();
            return;
        }
        this.tableNames = tableNames;
        StringBuilder builder = new StringBuilder();
        for (String table : tableNames) {
            builder.append("truncate table ").append(table).append(" restart identity cascade;\n");
        }
        clearSql = builder.toString();
    }

    @Override
    @SneakyThrows
    public Map<Object, Object> getCurrentValue() {
        Map<Object, Object> currentValue = new LinkedHashMap<>();
        @Cleanup Statement statement = connection.createStatement();
        for (String table : tableNames) {
            @Cleanup ResultSet resultSet = statement.executeQuery("SELECT * FROM " + table);
            ResultSetMetaData metaData = resultSet.getMetaData();
            while (resultSet.next()) {
                Map<String, Object> record = new LinkedHashMap<>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    Object object = resultSet.getObject(i);
                    if (object instanceof Timestamp timestamp) {
                        object = timestamp.toLocalDateTime();
                    } else if (object instanceof Date date) {
                        object = date.toLocalDate();
                    }
                    record.put(metaData.getColumnName(i), object);
                }
                StringBuilder key = new StringBuilder(table);
                for (String keyColumn : getPrimaryKey(table)) {
                    key.append("_").append(record.get(keyColumn));
                }
                currentValue.put(key.toString(), record);
            }
        }
        return currentValue;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    @SneakyThrows
    public void clear() {
        if (clearSql != null) {
            @Cleanup Statement statement = connection.createStatement();
            statement.execute(clearSql);
        }
    }

    private List<String> getPrimaryKey(String tableName) throws SQLException {
        if (primaryKeys == null) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            primaryKeys = new HashMap<>();
            @Cleanup ResultSet resultSet = databaseMetaData.getPrimaryKeys(null, null, null);
            while (resultSet.next()) {
                List<String> key = primaryKeys.computeIfAbsent(resultSet.getString("TABLE_NAME"),
                        (s) -> new ArrayList<>());
                key.add(resultSet.getString("COLUMN_NAME"));
            }
        }
        List<String> keys = primaryKeys.get(tableName);
        if (keys == null) {
            throw new RuntimeException(String.format("Table %s do not have a primary key", tableName));
        }
        return keys;
    }
}

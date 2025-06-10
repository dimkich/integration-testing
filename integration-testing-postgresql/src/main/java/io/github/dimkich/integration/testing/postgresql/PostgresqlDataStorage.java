package io.github.dimkich.integration.testing.postgresql;

import io.github.dimkich.integration.testing.dbunit.TruncateCascadeTableOperation;
import io.github.dimkich.integration.testing.postgresql.dbunit.CustomPostgresqlDataTypeFactory;
import io.github.dimkich.integration.testing.postgresql.dbunit.DisableTriggersOperation;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorage;
import io.github.dimkich.integration.testing.storage.sql.TableRestrictionBuilder;
import io.github.dimkich.integration.testing.util.StringUtils;
import lombok.Cleanup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.CompositeOperation;
import org.dbunit.operation.DatabaseOperation;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PostgresqlDataStorage implements SQLDataStorage {
    private static final DatabaseOperation CLEAN_INSERT = new DisableTriggersOperation(
            new CompositeOperation(new TruncateCascadeTableOperation(), DatabaseOperation.INSERT));

    @Getter
    private final String name;
    private final Connection connection;
    private final String adminUsername;

    private final String allowedTablesTable = "allowed_tables_" + StringUtils.randomString(16);
    private final PostgreTableRestrictionBuilder builder = new PostgreTableRestrictionBuilder();

    private Map<String, List<String>> primaryKeys;
    private IDatabaseConnection dbUnitConnection;
    private String clearSql;

    @Override
    public void executeSql(Collection<String> sql) throws Exception {
        @Cleanup Statement statement = connection.createStatement();
        statement.execute(String.join(";\n", sql));
    }

    @Override
    public Map<String, Map<String, Object>> getTablesData(Collection<String> tables) throws Exception {
        Map<String, Map<String, Object>> currentValue = new LinkedHashMap<>();
        @Cleanup Statement statement = connection.createStatement();
        for (String table : tables) {
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

    @Override
    public void loadDataset(IDataSet dataSet) throws DatabaseUnitException, SQLException {
        CLEAN_INSERT.execute(getDbUnitConnection(), dataSet);
    }

    @Override
    public DatabaseConfig getDbunitConfig() throws Exception {
        return getDbUnitConnection().getConfig();
    }

    private IDatabaseConnection getDbUnitConnection() throws DatabaseUnitException {
        if (dbUnitConnection == null) {
            dbUnitConnection = new DatabaseConnection(connection, null);
            dbUnitConnection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY,
                    new CustomPostgresqlDataTypeFactory());
            dbUnitConnection.getConfig().setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);
        }
        return dbUnitConnection;
    }

    @Override
    public Set<String> initTableRestriction() throws Exception {
        StringBuilder builder = new StringBuilder("""
                create table if not exists :tableName
                (
                  name text not null,
                    constraint :tableName_pk
                        primary key (name)
                );
                grant select on :tableName to public;
                create or replace function testsRestrictedTableChanges()
                    returns trigger
                    language plpgsql
                as
                $$
                DECLARE curr_user TEXT;
                begin
                    curr_user := (select current_user limit 1);
                    IF curr_user=':adminUsername' THEN
                        return null;
                    END IF;
                    PERFORM * FROM :tableName where name = tg_table_name LIMIT 1;
                    IF NOT FOUND THEN
                        RAISE EXCEPTION 'Changes to table \"%\" restricted, since it was not declared in tests \"init\" section.', tg_table_name;
                    END IF;
                    RETURN NULL;
                end;
                $$;
                """.replace(":tableName", allowedTablesTable)
                .replace(":adminUsername", adminUsername)
        );

        @Cleanup ResultSet rs = connection.getMetaData().getTables(null, null, null,
                new String[]{"TABLE"});
        Set<String> tables = new HashSet<>();
        while (rs.next()) {
            String name = rs.getString("TABLE_NAME");
            tables.add(name);
            builder.append("create trigger ").append(name).append("_tests_restricted\n")
                    .append("before insert or update or truncate or delete on ").append(name).append("\n")
                    .append("for each statement execute function testsRestrictedTableChanges();\n");
        }

        @Cleanup Statement statement = connection.createStatement();
        statement.execute(builder.toString());
        return tables;
    }

    @Override
    public TableRestrictionBuilder getRestrictionBuilder() {
        return builder;
    }

    @Override
    public void setTablesToClear(Collection<String> tablesToClear) {
        if (tablesToClear == null || tablesToClear.isEmpty()) {
            clearSql = null;
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (String table : tablesToClear) {
            builder.append("truncate table ").append(table).append(" restart identity cascade;\n");
        }
        clearSql = builder.toString();
    }

    @Override
    public void clearTables() throws Exception {
        if (clearSql != null) {
            @Cleanup Statement statement = connection.createStatement();
            statement.execute(clearSql);
        }
    }

    private class PostgreTableRestrictionBuilder implements TableRestrictionBuilder {
        private final StringBuilder builder = new StringBuilder();

        @Override
        public void allowTable(String table) {
            builder.append("insert into ").append(allowedTablesTable).append(" values ('").append(table)
                    .append("');\n");
        }

        @Override
        public void restrictTable(String table) {
            builder.append("delete from ").append(allowedTablesTable).append(" where name ='").append(table)
                    .append("';\n");
        }

        @Override
        public void finish() throws SQLException {
            if (!builder.isEmpty()) {
                @Cleanup Statement statement = connection.createStatement();
                statement.execute(builder.toString());
                builder.setLength(0);
            }
        }
    }
}

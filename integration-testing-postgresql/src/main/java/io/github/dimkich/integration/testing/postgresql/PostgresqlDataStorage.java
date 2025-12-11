package io.github.dimkich.integration.testing.postgresql;

import io.github.dimkich.integration.testing.dbunit.DeleteFromTableOperation;
import io.github.dimkich.integration.testing.postgresql.dbunit.CustomPostgresqlDataTypeFactory;
import io.github.dimkich.integration.testing.postgresql.dbunit.DisableTriggersOperation;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorage;
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
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;

import java.sql.Date;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PostgresqlDataStorage implements SQLDataStorage {
    private static final DatabaseOperation CLEAN_INSERT = new DisableTriggersOperation(
            new CompositeOperation(new DeleteFromTableOperation(), DatabaseOperation.INSERT));

    @Getter
    private final String name;
    private final Connection connection;
    private final String adminUsername;

    private final String allowedTablesTable = "allowed_tables_" + StringUtils.randomString(16).toLowerCase();
    private final Map<String, String> tableSequences = new HashMap<>();

    private Map<String, List<String>> primaryKeys;
    private IDatabaseConnection dbUnitConnection;

    @Override
    public void executeSql(Collection<String> sql) throws Exception {
        @Cleanup Statement statement = connection.createStatement();
        statement.execute(String.join(";\n", sql));
    }

    @Override
    public Map<String, Object> getTablesData(Collection<String> tables, Map<String, Set<String>> excludedRows) throws Exception {
        Map<String, Object> currentValue = new LinkedHashMap<>();
        @Cleanup Statement statement = connection.createStatement();
        for (String table : tables) {
            @Cleanup ResultSet resultSet = statement.executeQuery("SELECT * FROM " + table);
            ResultSetMetaData metaData = resultSet.getMetaData();
            Set<String> exclude = excludedRows.get(table);
            while (resultSet.next()) {
                Map<String, Object> record = new LinkedHashMap<>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    if (exclude != null && exclude.contains(metaData.getColumnName(i))) {
                        continue;
                    }
                    Object object = resultSet.getObject(i);
                    if (object instanceof Timestamp timestamp) {
                        object = timestamp.toLocalDateTime();
                    } else if (object instanceof Date date) {
                        object = date.toLocalDate();
                    } else if (object instanceof PgArray pgArray) {
                        object = pgArray.toString();
                    } else if (object instanceof PGobject pgObject) {
                        object = pgObject.toString();
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
    public Set<String> getTables() throws SQLException {
        @Cleanup ResultSet rs = connection.getMetaData().getTables(null, null, null,
                new String[]{"TABLE"});
        Set<String> tables = new LinkedHashSet<>();
        while (rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            if (!tableName.equals(allowedTablesTable)) {
                tables.add(tableName);
            }
        }
        tableSequences.clear();
        String sql = """
                SELECT
                    t.relname AS table,
                    s.relname AS seq
                FROM
                    pg_namespace tns
                        JOIN pg_class t ON tns.oid = t.relnamespace
                        AND t.relkind IN ('p', 'r')
                        JOIN pg_depend d ON t.oid = d.refobjid
                        JOIN pg_class s ON d.objid = s.oid and s.relkind = 'S'
                        JOIN pg_namespace sns ON s.relnamespace = sns.oid;
                """;
        @Cleanup Statement statement = connection.createStatement();
        @Cleanup ResultSet resultSet = statement.executeQuery(sql);
        while (resultSet.next()) {
            tableSequences.put(resultSet.getString("table"), resultSet.getString("seq"));
        }
        return tables;
    }

    @Override
    public void initTablesRestriction(Collection<String> tables) throws SQLException {
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

        for (String name : tables) {
            builder.append("create trigger ").append(name).append("_tests_restricted\n")
                    .append("before insert or update or truncate or delete on ").append(name).append("\n")
                    .append("for each statement execute function testsRestrictedTableChanges();\n");
        }

        @Cleanup Statement statement = connection.createStatement();
        statement.execute(builder.toString());
    }

    @Override
    public String getAllowTableSql(String table) {
        return "insert into " + allowedTablesTable + " values ('" + table + "')";
    }

    @Override
    public String getRestrictTableSql(String table) {
        return "delete from " + allowedTablesTable + " where name ='" + table + "'";
    }

    @Override
    public String getClearSql(Collection<String> tables) {
        StringBuilder builder = new StringBuilder();
        builder.append("SET session_replication_role = 'replica';\n");
        for (String table : tables) {
            builder.append("DELETE FROM ").append(table).append(";\n");
        }
        builder.append("SET session_replication_role = 'origin'");
        return builder.toString();
    }

    @Override
    public String getRestartIdentitySql(Collection<String> tables) {
        return tables.stream().map(tableSequences::get).filter(Objects::nonNull)
                .map(s -> "ALTER SEQUENCE " + s + " RESTART").collect(Collectors.joining(";\n"));
    }
}

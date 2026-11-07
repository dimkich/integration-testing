package io.github.dimkich.integration.testing.postgresql;

import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import io.github.dimkich.integration.testing.dbunit.DbUnitFixture;
import io.github.dimkich.integration.testing.dbunit.DeleteFromTableOperation;
import io.github.dimkich.integration.testing.postgresql.config.PostgresProperties;
import io.github.dimkich.integration.testing.postgresql.dbunit.DisableTriggersOperation;
import io.github.dimkich.integration.testing.postgresql.dbunit.PostgresFixtureDataTypeFactory;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.operation.CompositeOperation;
import org.dbunit.operation.DatabaseOperation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Owns the JDBC connection of one PostgreSQL test storage and the operations that need
 * session-level control: statement execution, result-set mapping, audit transitions and DBUnit
 * fixture loading.
 */
@RequiredArgsConstructor
public class PostgresDatabase {
    private static final String CLEAR_NODE_TAG = "SELECT set_config('it.node', '', false)";
    private static final DatabaseOperation CLEAN_INSERT = new DisableTriggersOperation(
            new CompositeOperation(new DeleteFromTableOperation(), DatabaseOperation.INSERT));

    private final PostgresProperties.Connection connectionProperties;

    private Connection connection;
    private IDatabaseConnection dbUnitConnection;

    @SuppressWarnings("SqlSourceToSinkFlow")
    public void execute(String sql) throws SQLException {
        @Cleanup Statement statement = getConnection().createStatement();
        statement.execute(sql);
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    public <T> List<T> query(String sql, SneakyFunction<ResultSet, T, SQLException> mapper)
            throws SQLException {
        @Cleanup Statement statement = getConnection().createStatement();
        if (!statement.execute(sql)) {
            throw new SQLException("SQL did not return a result set");
        }
        @Cleanup ResultSet resultSet = statement.getResultSet();
        if (resultSet == null) {
            throw new SQLException("SQL returned no result set");
        }
        return map(resultSet, mapper);
    }

    public <T> List<T> query(String sql,
                             SneakyConsumer<PreparedStatement, SQLException> binder,
                             SneakyFunction<ResultSet, T, SQLException> mapper) throws SQLException {
        return extract(sql, binder, resultSet -> map(resultSet, mapper));
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    public <T> T extract(String sql,
                         SneakyConsumer<PreparedStatement, SQLException> binder,
                         SneakyFunction<ResultSet, T, SQLException> extractor) throws SQLException {
        @Cleanup PreparedStatement statement = getConnection().prepareStatement(sql);
        binder.accept(statement);
        @Cleanup ResultSet resultSet = statement.executeQuery();
        return extractor.apply(resultSet);
    }

    public void executeTransition(Collection<String> cleanupSql, String[] taggedSql)
            throws SQLException {
        if (cleanupSql.isEmpty() && taggedSql.length == 0) {
            return;
        }
        @Cleanup Statement statement = getConnection().createStatement();
        executeAndResetOnFailure(statement, transitionSql(cleanupSql, taggedSql));
    }

    public void loadFixture(DbUnitFixture fixture, TableSet tables)
            throws DatabaseUnitException, SQLException {
        CLEAN_INSERT.execute(getDbUnitConnection(), fixture.filteredDataSet(tables));
    }

    public void resetFixture() {
        dbUnitConnection = null;
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(getUrl(), connectionProperties.getUser(),
                    connectionProperties.getPassword());
            connection.setAutoCommit(true);
            dbUnitConnection = null;
        }
        return connection;
    }

    private IDatabaseConnection getDbUnitConnection() throws DatabaseUnitException, SQLException {
        if (dbUnitConnection == null) {
            String defaultSchema = connectionProperties.getDefaultSchema();
            dbUnitConnection = new DatabaseConnection(getConnection(), defaultSchema);
            DatabaseConfig config = dbUnitConnection.getConfig();
            config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY,
                    new PostgresFixtureDataTypeFactory());
            config.setProperty(DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, true);
            config.setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);
            config.setProperty(DatabaseConfig.FEATURE_BATCHED_STATEMENTS, true);
            config.setProperty(DatabaseConfig.PROPERTY_BATCH_SIZE,
                    Math.max(1, connectionProperties.getBatchSize()));
        }
        return dbUnitConnection;
    }

    private String getUrl() {
        return "jdbc:postgresql://" + connectionProperties.getHost() + ":"
                + connectionProperties.getPort() + "/" + connectionProperties.getDatabase();
    }

    private static <T> List<T> map(ResultSet resultSet,
                                   SneakyFunction<ResultSet, T, SQLException> mapper)
            throws SQLException {
        List<T> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(mapper.apply(resultSet));
        }
        return result;
    }

    private static void executeAndResetOnFailure(Statement statement, String sql)
            throws SQLException {
        try {
            statement.execute(sql);
        } catch (SQLException e) {
            try {
                statement.execute("SET session_replication_role = 'origin';\n" + nodeTagSql(null));
            } catch (SQLException reset) {
                e.addSuppressed(reset);
            }
            throw e;
        }
    }

    private static String transitionSql(Collection<String> cleanupSql, String[] taggedSql) {
        List<String> sql = new ArrayList<>(cleanupSql.size() + taggedSql.length * 2 + 4);
        sql.add(nodeTagSql(null));
        if (!cleanupSql.isEmpty()) {
            sql.add("SET session_replication_role = 'replica'");
            sql.addAll(cleanupSql);
            sql.add("SET session_replication_role = 'origin'");
        }
        for (int tag = 0; tag < taggedSql.length; tag++) {
            sql.add(nodeTagSql(tag));
            sql.add(taggedSql[tag]);
        }
        sql.add(nodeTagSql(null));
        return String.join(";\n", sql);
    }

    private static String nodeTagSql(Integer tag) {
        return tag == null
                ? CLEAR_NODE_TAG
                : "SELECT set_config('it.node', '" + tag + "', false)";
    }
}

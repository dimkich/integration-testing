package io.github.dimkich.integration.testing.postgresql.dbunit;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.DatabaseOperation;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Runs a DBUnit operation with PostgreSQL triggers disabled by switching
 * {@code session_replication_role} to {@code replica} for the operation and restoring it to
 * {@code origin} afterwards. Fixture loading must not record its own writes in the audit journal.
 * <p>
 * The connection role needs permission to change {@code session_replication_role}.
 */
@RequiredArgsConstructor
public class DisableTriggersOperation extends DatabaseOperation {
    private static final String DISABLE_TRIGGERS = "SET session_replication_role = 'replica';";
    private static final String ENABLE_TRIGGERS = "SET session_replication_role = 'origin';";

    private final DatabaseOperation operation;

    @Override
    public void execute(IDatabaseConnection connection, IDataSet dataSet)
            throws DatabaseUnitException, SQLException {
        executeSql(connection, DISABLE_TRIGGERS);
        try {
            operation.execute(connection, dataSet);
        } catch (DatabaseUnitException | SQLException | RuntimeException | Error e) {
            restoreTriggers(connection, e);
            throw e;
        }
        executeSql(connection, ENABLE_TRIGGERS);
    }

    private void restoreTriggers(IDatabaseConnection connection, Throwable failure) {
        try {
            executeSql(connection, ENABLE_TRIGGERS);
        } catch (SQLException reset) {
            failure.addSuppressed(reset);
        }
    }

    private void executeSql(IDatabaseConnection connection, String sql) throws SQLException {
        @Cleanup Statement statement = connection.getConnection().createStatement();
        statement.execute(sql);
    }
}

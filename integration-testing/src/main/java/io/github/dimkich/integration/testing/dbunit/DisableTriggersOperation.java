package io.github.dimkich.integration.testing.dbunit;

import lombok.RequiredArgsConstructor;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.DatabaseOperation;

import java.sql.SQLException;
import java.sql.Statement;

@RequiredArgsConstructor
public class DisableTriggersOperation extends DatabaseOperation {
    private final DatabaseOperation operation;

    @Override
    public void execute(IDatabaseConnection connection, IDataSet dataSet) throws DatabaseUnitException, SQLException {
        executeSql(connection, "SET session_replication_role = 'replica';");
        try {
            operation.execute(connection, dataSet);
        } finally {
            executeSql(connection, "SET session_replication_role = 'origin';");
        }
    }

    private void executeSql(IDatabaseConnection connection, String sql) throws SQLException {
        try (Statement statement = connection.getConnection().createStatement()) {
            statement.execute(sql);
        }
    }
}

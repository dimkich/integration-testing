package io.github.dimkich.integration.testing.dbunit;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.statement.IBatchStatement;
import org.dbunit.database.statement.IStatementFactory;
import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.AbstractOperation;

import java.sql.SQLException;

public class DeleteFromTableOperation extends AbstractOperation {
    @Override
    public void execute(IDatabaseConnection connection, IDataSet dataSet) throws DatabaseUnitException, SQLException {
        DatabaseConfig databaseConfig = connection.getConfig();
        IStatementFactory statementFactory = (IStatementFactory) databaseConfig.getProperty(DatabaseConfig.PROPERTY_STATEMENT_FACTORY);
        IBatchStatement statement = statementFactory.createBatchStatement(connection);

        for (String tableName : dataSet.getTableNames()) {
            statement.addBatch("delete from " + getQualifiedName(connection.getSchema(), tableName, connection));
        }

        statement.executeBatch();
        statement.clearBatch();
        statement.close();
    }
}
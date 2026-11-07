package io.github.dimkich.integration.testing.dbunit;

import lombok.Cleanup;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.statement.IBatchStatement;
import org.dbunit.database.statement.IStatementFactory;
import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.AbstractOperation;

import java.sql.SQLException;

/**
 * Base operation for table-wide batch statements. Subclasses only provide the SQL for one
 * qualified table name.
 */
abstract class BatchTableOperation extends AbstractOperation {
    @Override
    public void execute(IDatabaseConnection connection, IDataSet dataSet)
            throws DatabaseUnitException, SQLException {
        DatabaseConfig databaseConfig = connection.getConfig();
        IStatementFactory statementFactory = (IStatementFactory) databaseConfig.getProperty(
                DatabaseConfig.PROPERTY_STATEMENT_FACTORY);
        @Cleanup IBatchStatement statement = statementFactory.createBatchStatement(connection);

        for (String tableName : dataSet.getTableNames()) {
            statement.addBatch(createSql(
                    getQualifiedName(connection.getSchema(), tableName, connection)));
        }

        statement.executeBatch();
    }

    protected abstract String createSql(String qualifiedName);
}

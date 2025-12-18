package io.github.dimkich.integration.testing.storage.sql;

import jakarta.annotation.Nullable;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Factory for creating JDBC {@link Connection} instances and {@link SQLDataStorage}
 * wrappers used by integration tests.
 */
public interface SQLDataStorageFactory {

    /**
     * Returns the fully qualified name of the JDBC driver class that should be used
     * with connections created by this factory.
     *
     * @return JDBC driver class name
     */
    String getDriverClassName();

    /**
     * Creates a new JDBC {@link Connection} for the given URL.
     *
     * @param url                 JDBC URL to connect to
     * @param suggestedProperties optional Spring {@link DataSourceProperties} that can
     *                            be used as a hint when configuring the connection; may be {@code null}
     * @return a new JDBC connection
     * @throws SQLException if the connection cannot be created
     */
    Connection createConnection(String url, @Nullable DataSourceProperties suggestedProperties) throws SQLException;

    /**
     * Creates a new {@link SQLDataStorage} wrapper for the provided connection.
     *
     * @param name       logical storage name, typically used for logging and identification
     * @param connection an open JDBC connection
     * @param username   username associated with the storage/connection
     * @return a new SQL data storage instance
     */
    SQLDataStorage createStorage(String name, Connection connection, String username);
}

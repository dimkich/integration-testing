package io.github.dimkich.integration.testing.storage.sql;

import jakarta.annotation.Nullable;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import java.sql.Connection;
import java.sql.SQLException;

public interface SQLDataStorageFactory {
    String getDriverClassName();

    Connection createConnection(String url, @Nullable DataSourceProperties suggestedProperties) throws SQLException;

    SQLDataStorage createStorage(String name, Connection connection, String username);
}

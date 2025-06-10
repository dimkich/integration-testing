package io.github.dimkich.integration.testing.postgresql;

import com.playtika.testcontainer.postgresql.PostgreSQLProperties;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorage;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageFactory;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.postgresql.Driver;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import java.sql.Connection;
import java.sql.SQLException;

@RequiredArgsConstructor
public class PostgresqlDataStorageFactory implements SQLDataStorageFactory {
    private final PostgreSQLProperties properties;

    @Override
    public String getDriverClassName() {
        return Driver.class.getName();
    }

    @Override
    public Connection createConnection(String url, @Nullable DataSourceProperties suggestedProperties) throws SQLException {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        if (suggestedProperties != null) {
            ds.setUrl(suggestedProperties.getUrl());
            ds.setUser(suggestedProperties.getUsername());
            ds.setPassword(suggestedProperties.getPassword());
        } else {
            ds.setUrl(url);
            ds.setUser(properties.getUser());
            ds.setPassword(properties.getPassword());
        }
        return ds.getConnection();
    }

    @Override
    public SQLDataStorage createStorage(String name, Connection connection, String username) {
        return new PostgresqlDataStorage(name, connection, username);
    }
}

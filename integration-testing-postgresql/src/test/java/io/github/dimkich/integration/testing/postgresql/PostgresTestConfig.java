package io.github.dimkich.integration.testing.postgresql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

@EnableAutoConfiguration
@Configuration
public class PostgresTestConfig {

    @Bean
    public TestSetupModule postgresqlTestModule() {
        return new TestSetupModule().addSubTypes(PostgresSerdeDto.class);
    }

    @Bean
    public PostgresPointProvider postgresPointProvider() {
        return new PostgresPointProvider();
    }

    @Bean
    public PostgresByteProvider postgresByteProvider() {
        return new PostgresByteProvider();
    }

    @Bean
    public DataSource dataSource(@Value("${embedded.postgresql.host}") String host,
                                 @Value("${embedded.postgresql.port}") int port,
                                 @Value("${embedded.postgresql.schema}") String database,
                                 @Value("${app.datasource.user}") String user,
                                 @Value("${app.datasource.password}") String password) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL("jdbc:postgresql://" + host + ":" + port + "/" + database);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        return new AbstractDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return utc(dataSource.getConnection());
            }

            @Override
            public Connection getConnection(String username, String pwd) throws SQLException {
                return utc(dataSource.getConnection(username, pwd));
            }

            private Connection utc(Connection connection) throws SQLException {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET TIME ZONE 'UTC'");
                }
                return connection;
            }
        };
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public PostgresFacade postgresqlFacade(JdbcTemplate jdbcTemplate, SQLDataStorageService dataStorageService) {
        return new PostgresFacade(jdbcTemplate, dataStorageService);
    }

    @Bean
    public CacheHook cacheHook() {
        return new CacheHook();
    }

    @Bean
    public ClearTablesHook clearTablesHook(JdbcTemplate jdbcTemplate, TestDataStorages testDataStorages) {
        return new ClearTablesHook(jdbcTemplate, testDataStorages);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public XmlMapper xmlMapper() {
        return new XmlMapper();
    }
}

package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.TestDataStorage;
import io.github.dimkich.integration.testing.execution.MockInvokeConfig;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import io.github.dimkich.integration.testing.storage.keyvalue.KeyValueOperationsConfig;
import io.github.dimkich.integration.testing.storage.mapping.StorageMappingConfig;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageFactory;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import jakarta.annotation.PostConstruct;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(JdbcUtils.class)
@Import({TestDataStorages.class, ObjectsDifference.class, MockInvokeConfig.class, StorageMappingConfig.class})
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {
    private final ConfigurableListableBeanFactory beanFactory;
    private final TestExecutor testExecutor;
    @Autowired(required = false)
    private final LiquibaseProperties liquibaseProperties;
    @Autowired(required = false)
    private final FlywayProperties flywayProperties;
    private final List<SQLDataStorageFactory> factoriesList;
    private Map<String, SQLDataStorageFactory> factoriesMap;

    @PostConstruct
    void init() {
        factoriesMap = factoriesList.stream()
                .collect(Collectors.toMap(SQLDataStorageFactory::getDriverClassName, Function.identity()));
    }

    public TestDataStorage createDataSourceStorage(String name, DataSource dataSource) throws SQLException {
        @Cleanup Connection connection = dataSource.getConnection();
        String url = connection.getMetaData().getURL();
        String username = connection.getMetaData().getUserName();
        String product = connection.getMetaData().getDatabaseProductName();
        DatabaseDriver databaseDriver = DatabaseDriver.fromProductName(JdbcUtils.commonDatabaseName(product));
        SQLDataStorageFactory factory = factoriesMap.get(databaseDriver.getDriverClassName());
        if (factory == null) {
            factory = factoriesMap.get(DatabaseDriver.fromJdbcUrl(url).getDriverClassName());
        }
        if (factory == null) {
            throw new SQLException("Unsupported database driver: " + databaseDriver);
        }
        DataSourceProperties properties = null;
        if (liquibaseProperties != null && !username.equals(liquibaseProperties.getUser())) {
            properties = new DataSourceProperties();
            properties.setUsername(liquibaseProperties.getUser());
            properties.setPassword(liquibaseProperties.getPassword());
        } else if (flywayProperties != null && !username.equals(flywayProperties.getUser())) {
            properties = new DataSourceProperties();
            properties.setUsername(flywayProperties.getUser());
            properties.setPassword(flywayProperties.getPassword());
        }
        if (properties != null) {
            properties.setUrl(url);
            properties.setDriverClassName(databaseDriver.getDriverClassName());
        }
        Connection adminConnection = factory.createConnection(url, properties);
        String newUser = adminConnection.getMetaData().getUserName();
        if (newUser.equals(username)) {
            throw new SQLException("Cannot use one username in admin and regular connections");
        }
        return new SQLDataStorageService(factory.createStorage(name, adminConnection, newUser), beanFactory,
                testExecutor);
    }

    @Configuration
    @Import(KeyValueOperationsConfig.class)
    @EnableConfigurationProperties(LiquibaseProperties.class)
    public static class DataSourceConfig implements BeanFactoryPostProcessor {
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            String factoryBean = beanFactory.getBeanNamesForType(StorageConfig.class)[0];
            for (String name : beanFactory.getBeanNamesForType(DataSource.class)) {
                AbstractBeanDefinition definition = BeanDefinitionBuilder.rootBeanDefinition(TestDataStorage.class)
                        .setFactoryMethodOnBean("createDataSourceStorage", factoryBean)
                        .addConstructorArgValue(name)
                        .addConstructorArgReference(name)
                        .getBeanDefinition();
                ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition("#" + name, definition);
            }
        }
    }
}

package io.github.dimkich.integration.testing.storage;

import com.zaxxer.hikari.HikariConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
@Import({TestDataStorages.class, ObjectsDifference.class})
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {
    @Autowired(required = false)
    private LiquibaseProperties liquibaseProperties;

    public DataSourceStorage createDataSourceStorage(String name, DataSource dataSource) throws SQLException {
        String url = null;
        String username = null;
        String password = null;
        if (liquibaseProperties != null) {
            url = liquibaseProperties.getUrl();
            username = liquibaseProperties.getUser();
            password = liquibaseProperties.getPassword();
        }
        if (dataSource instanceof HikariConfig config) {
            url = url == null ? config.getJdbcUrl() : url;
            username = username == null ? config.getUsername() : username;
            password = password == null ? config.getPassword() : password;
        }
        DataSourceBuilder<?> builder = DataSourceBuilder.create().type(SimpleDriverDataSource.class)
                .url(url).username(username).password(password);
        DataSource ds = builder.build();
        Connection connection = ds.getConnection();
        return new DataSourceStorage(name, connection, new TablesRestrictionService(connection, username));
    }

    @Configuration
    @EnableConfigurationProperties(LiquibaseProperties.class)
    public static class DataSourceConfig implements BeanFactoryPostProcessor {
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            String factoryBean = beanFactory.getBeanNamesForType(StorageConfig.class)[0];
            for (String name : beanFactory.getBeanNamesForType(DataSource.class)) {
                AbstractBeanDefinition definition = BeanDefinitionBuilder.rootBeanDefinition(DataSourceStorage.class)
                        .setFactoryMethodOnBean("createDataSourceStorage", factoryBean)
                        .addConstructorArgValue(name)
                        .addConstructorArgReference(name)
                        .getBeanDefinition();
                ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition("#" + name, definition);
            }
        }
    }
}

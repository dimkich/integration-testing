package io.github.dimkich.integration.testing.initialization.sql;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.initialization.TestInit;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Set;

/**
 * Represents a SQL storage setup configuration for integration tests.
 * This class extends {@link io.github.dimkich.integration.testing.initialization.TestInit}
 * and provides a mechanism to configure SQL storage systems by executing SQL scripts,
 * loading DBUnit XML datasets, and setting up table hooks for automatic data reloading.
 *
 * <p>SQL storage setup is typically used once per test container to configure the storage
 * with initial SQL scripts, DBUnit datasets, and table hooks. This setup runs before
 * individual test cases and parts, allowing you to prepare the database schema and
 * define hooks that will be triggered when specific tables are modified.
 *
 * <p>Example XML configurations:
 * <pre>{@code
 * <!-- Basic setup with DBUnit dataset and table hook -->
 * <init type="sqlStorageSetup" name="dataSource">
 *     <dbUnitPath>initializationData.xml</dbUnitPath>
 *     <tableHook tableName="t1" beanName="testSQLDataStorage" beanMethod="reloadT1"/>
 * </init>
 *
 * <!-- Setup with SQL file paths -->
 * <init type="sqlStorageSetup" name="dataSource">
 *     <sqlFilePath>schema.sql</sqlFilePath>
 *     <sqlFilePath>test-data.sql</sqlFilePath>
 *     <dbUnitPath>initializationData.xml</dbUnitPath>
 * </init>
 *
 * <!-- Setup with inline SQL statements -->
 * <init type="sqlStorageSetup" name="dataSource">
 *     <sql>CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, name VARCHAR(100))</sql>
 *     <sql>INSERT INTO users VALUES (1, 'John')</sql>
 *     <dbUnitPath>users.xml</dbUnitPath>
 * </init>
 *
 * <!-- Setup with multiple table hooks -->
 * <init type="sqlStorageSetup" name="dataSource">
 *     <dbUnitPath>initializationData.xml</dbUnitPath>
 *     <tableHook tableName="t1" beanName="testSQLDataStorage" beanMethod="reloadT1"/>
 *     <tableHook tableName="t2" beanName="testSQLDataStorage" beanMethod="reloadT2"/>
 * </init>
 *
 * <!-- Setup with SQL files and inline SQL combined -->
 * <init type="sqlStorageSetup" name="dataSource">
 *     <sqlFilePath>schema.sql</sqlFilePath>
 *     <sql>ALTER TABLE users ADD COLUMN email VARCHAR(255)</sql>
 *     <dbUnitPath>users.xml</dbUnitPath>
 * </init>
 *
 * <!-- Apply to specific test type -->
 * <init type="sqlStorageSetup" name="dataSource" applyTo="TestCase">
 *     <dbUnitPath>test-case-data.xml</dbUnitPath>
 * </init>
 * }</pre>
 *
 * @author dimkich
 * @see io.github.dimkich.integration.testing.initialization.TestInit
 * @see io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService
 */
@Getter
@Setter
@ToString
public class SqlStorageSetup extends TestInit {
    /**
     * The name of the SQL storage to configure.
     * This should match the name of a configured {@link io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService}
     * in the application context.
     * <p>
     * This is serialized as an XML attribute when using Jackson XML serialization.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="sqlStorageSetup" name="dataSource"/>
     * }</pre>
     */
    @JacksonXmlProperty(isAttribute = true)
    private String name;

    /**
     * List of file paths to SQL scripts that should be executed during setup.
     * The paths are relative to the classpath and will be loaded as resources.
     * SQL files are executed before inline SQL statements.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="sqlStorageSetup" name="dataSource">
     *     <sqlFilePath>schema.sql</sqlFilePath>
     *     <sqlFilePath>test-data.sql</sqlFilePath>
     * </init>
     * }</pre>
     */
    private List<String> sqlFilePath;

    /**
     * List of inline SQL statements to execute during setup.
     * These SQL statements are executed after SQL files from {@code sqlFilePath}.
     * Each element in the list represents a single SQL statement.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="sqlStorageSetup" name="dataSource">
     *     <sql>CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100))</sql>
     *     <sql>INSERT INTO users VALUES (1, 'John')</sql>
     * </init>
     * }</pre>
     */
    private List<String> sql;

    /**
     * Set of file paths to DBUnit XML datasets that should be loaded during setup.
     * The paths are relative to the classpath and will be loaded as resources.
     * DBUnit datasets define initial data that can be loaded into tables during test execution.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="sqlStorageSetup" name="dataSource">
     *     <dbUnitPath>initializationData.xml</dbUnitPath>
     *     <dbUnitPath>users.xml</dbUnitPath>
     * </init>
     * }</pre>
     */
    private Set<String> dbUnitPath;

    /**
     * List of table hooks that define methods to be called when specific tables are modified.
     * Table hooks allow you to automatically reload or refresh data in related beans
     * when certain tables are changed during test execution.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="sqlStorageSetup" name="dataSource">
     *     <tableHook tableName="t1" beanName="testSQLDataStorage" beanMethod="reloadT1"/>
     *     <tableHook tableName="t2" beanName="cacheService" beanMethod="refreshCache"/>
     * </init>
     * }</pre>
     */
    private List<TableHook> tableHook;

    /**
     * Represents a table hook configuration that associates a database table with a bean method
     * to be invoked when that table is modified during test execution.
     * <p>
     * Table hooks are useful for automatically refreshing cached data or reloading related
     * beans when specific tables are changed. The hook is triggered whenever the associated
     * table is modified through SQL operations or DBUnit dataset loading.
     *
     * <p>Example XML configurations:
     * <pre>{@code
     * <!-- Single table hook -->
     * <tableHook tableName="users" beanName="userService" beanMethod="reloadUsers"/>
     *
     * <!-- Multiple table hooks -->
     * <tableHook tableName="t1" beanName="testSQLDataStorage" beanMethod="reloadT1"/>
     * <tableHook tableName="t2" beanName="testSQLDataStorage" beanMethod="reloadT2"/>
     * }</pre>
     */
    @Data
    public static class TableHook {
        /**
         * The name of the database table that this hook is associated with.
         * When this table is modified during test execution, the specified bean method will be invoked.
         * <p>
         * This is serialized as an XML attribute when using Jackson XML serialization.
         * <p>
         * XML Example:
         * <pre>{@code
         * <tableHook tableName="users" beanName="userService" beanMethod="reloadUsers"/>
         * }</pre>
         */
        @JacksonXmlProperty(isAttribute = true)
        private String tableName;

        /**
         * The name of the Spring bean that contains the method to be invoked.
         * This should match a bean name in the Spring application context.
         * <p>
         * This is serialized as an XML attribute when using Jackson XML serialization.
         * <p>
         * XML Example:
         * <pre>{@code
         * <tableHook tableName="users" beanName="userService" beanMethod="reloadUsers"/>
         * }</pre>
         */
        @JacksonXmlProperty(isAttribute = true)
        private String beanName;

        /**
         * The name of the method to invoke on the specified bean when the table is modified.
         * The method should be accessible and should not require parameters.
         * <p>
         * This is serialized as an XML attribute when using Jackson XML serialization.
         * <p>
         * XML Example:
         * <pre>{@code
         * <tableHook tableName="users" beanName="userService" beanMethod="reloadUsers"/>
         * }</pre>
         */
        @JacksonXmlProperty(isAttribute = true)
        private String beanMethod;
    }
}

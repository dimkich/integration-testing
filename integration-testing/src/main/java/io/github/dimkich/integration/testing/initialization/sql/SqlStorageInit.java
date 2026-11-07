package io.github.dimkich.integration.testing.initialization.sql;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.initialization.TestInit;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Represents a SQL storage initialization configuration for integration tests.
 * This class extends {@link io.github.dimkich.integration.testing.initialization.TestInit}
 * and provides a mechanism to initialize SQL storage systems by clearing tables, loading data from
 * DBUnit datasets and executing SQL statements.
 *
 * <p>SQL storage initialization is used to configure which tables should be cleared or loaded with
 * data and to execute custom SQL statements. This initialization runs before individual test cases
 * and parts, allowing you to prepare the database state.
 *
 * <p>Example XML configurations:
 * <pre>{@code
 * <!-- Clear specific tables -->
 * <init type="sqlStorageInit" name="dataSource">
 *     <tablesToClear>t1,t2</tablesToClear>
 * </init>
 *
 * <!-- Load all tables from DBUnit dataset -->
 * <init type="sqlStorageInit" name="dataSource" loadAllTables="true"/>
 *
 * <!-- Clear and load specific tables -->
 * <init type="sqlStorageInit" name="dataSource">
 *     <tablesToClear>t1</tablesToClear>
 *     <tablesToLoad>t1</tablesToLoad>
 * </init>
 *
 * <!-- Execute SQL statements -->
 * <init type="sqlStorageInit" name="dataSource">
 *     <sql>update t1 set column = 1</sql>
 * </init>
 *
 * <!-- Multiple SQL statements -->
 * <init type="sqlStorageInit" name="dataSource">
 *     <tablesToClear>t1</tablesToClear>
 *     <sql>update t1 set column = 1</sql>
 *     <sql>insert into t1 values (2, 'test')</sql>
 * </init>
 *
 * <!-- Apply to specific test type -->
 * <init type="sqlStorageInit" name="dataSource" applyTo="TestCase">
 *     <tablesToClear>t1,t2</tablesToClear>
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
public class SqlStorageInit extends TestInit {
    /**
     * The name of the SQL storage to initialize.
     * This should match the name of a configured {@link io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService}
     * in the application context.
     * <p>
     * This is serialized as an XML attribute when using Jackson XML serialization.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="sqlStorageInit" name="dataSource"/>
     * }</pre>
     */
    @JacksonXmlProperty(isAttribute = true)
    private String name;

    /**
     * Whether to load all tables from the DBUnit dataset configured in {@link SqlStorageSetup}.
     * If set to {@code true}, all tables available in the DBUnit dataset will be loaded with data.
     * If set to {@code false} or {@code null}, only tables specified in {@code tablesToLoad} will be loaded.
     * <p>
     * This is serialized as an XML attribute when using Jackson XML serialization.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="sqlStorageInit" name="dataSource" loadAllTables="true"/>
     * }</pre>
     */
    @JacksonXmlProperty(isAttribute = true)
    private Boolean loadAllTables;

    /**
     * Comma-separated list of table names that should be cleared before test execution.
     * Tables specified here are set to the {@code CLEARED} state, unless they are also listed
     * in {@code tablesToLoad} or {@code loadAllTables} is set to {@code true}.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="sqlStorageInit" name="dataSource">
     *     <tablesToClear>t1,t2</tablesToClear>
     * </init>
     * }</pre>
     */
    private String tablesToClear;

    /**
     * Comma-separated list of table names that should be loaded with data from the DBUnit dataset.
     * Tables specified here will be loaded with their initial data from the dataset configured in
     * {@link SqlStorageSetup}. If not specified, no tables will be loaded unless {@code loadAllTables}
     * is set to {@code true}.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="sqlStorageInit" name="dataSource">
     *     <tablesToLoad>t1</tablesToLoad>
     * </init>
     * }</pre>
     */
    private String tablesToLoad;

    /**
     * List of SQL statements to execute during initialization.
     * These SQL statements are executed after table restrictions and data loading operations.
     * Each element in the list represents a single SQL statement.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="sqlStorageInit" name="dataSource">
     *     <sql>update t1 set column = 1</sql>
     *     <sql>insert into t1 values (2, 'test')</sql>
     * </init>
     * }</pre>
     */
    private List<String> sql;
}
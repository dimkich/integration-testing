package io.github.dimkich.integration.testing.postgresql.config;

import io.github.dimkich.integration.testing.dbunit.DbUnitFixture;
import io.github.dimkich.integration.testing.initialization.InitializationService;
import io.github.dimkich.integration.testing.postgresql.PostgresDataStorage;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import io.github.dimkich.integration.testing.storage.sql.SequenceResetScheduler;
import io.github.dimkich.integration.testing.storage.sql.StorageStateFactory;
import io.github.dimkich.integration.testing.storage.sql.StorageTransitionExecutor;
import io.github.dimkich.integration.testing.storage.sql.TableRevisionTracker;
import io.github.dimkich.integration.testing.storage.sql.state.TransitionAnalyzer;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;

/**
 * Factory bean for the connection specific PostgreSQL storage components.
 * <p>
 * The class is imported as a regular bean, so its bean name is the fully qualified class name.
 * {@link PostgresConfig.PostProcessor} relies on that stable name when it registers per-connection
 * bean definitions with factory methods.
 */
@RequiredArgsConstructor
public class PostgresBeanFactory {
    private final PostgresProperties postgresProperties;
    @Lazy
    private final InitializationService initializationService;

    /**
     * Returns the validated connection configuration for a storage name. The name comes from the
     * YAML {@code connections} key and is also used in {@code <init name="...">}.
     *
     * @param name connection name
     * @return the connection configuration
     * @throws IllegalStateException when the connection is not configured
     */
    @SuppressWarnings("unused")
    public PostgresProperties.Connection createConnectionProperties(String name) {
        PostgresProperties.Connection connection = postgresProperties.getConnections().get(name);
        if (connection == null) {
            throw new IllegalStateException(String.format("PostgreSQL connection '%s' is not configured", name));
        }
        return connection;
    }

    /**
     * Creates the table registry for one connection. Unqualified table names are resolved in the
     * connection's {@code defaultSchema}.
     *
     * @param connection connection configuration
     * @return a new table registry
     */
    @SuppressWarnings("unused")
    public TableRegistry createTableRegistry(PostgresProperties.Connection connection) {
        return new TableRegistry(connection.getDefaultSchema());
    }

    /**
     * Creates the {@link SQLDataStorageService} that exposes the PostgreSQL storage as the
     * framework's {@code TestDataStorage} under the connection name.
     *
     * @param storage PostgreSQL storage facade
     * @param tableRegistry canonical table registry
     * @param fixture DBUnit fixture holder
     * @param storageStateFactory storage state factory
     * @param transitionExecutor transition executor
     * @param revisions table revision tracker
     * @param sequenceResetScheduler sequence reset scheduler
     * @return a new storage service
     */
    @SuppressWarnings("unused")
    public SQLDataStorageService createDataStorageService(
            PostgresDataStorage storage,
            TableRegistry tableRegistry,
            DbUnitFixture fixture,
            StorageStateFactory storageStateFactory,
            StorageTransitionExecutor transitionExecutor,
            TableRevisionTracker revisions,
            SequenceResetScheduler sequenceResetScheduler) {
        return new SQLDataStorageService(storage, initializationService, tableRegistry, fixture,
                storageStateFactory, transitionExecutor, revisions, sequenceResetScheduler,
                new TransitionAnalyzer(tableRegistry));
    }
}

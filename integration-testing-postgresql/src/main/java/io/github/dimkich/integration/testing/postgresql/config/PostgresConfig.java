package io.github.dimkich.integration.testing.postgresql.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.dbunit.DbUnitFixture;
import io.github.dimkich.integration.testing.postgresql.PostgresDataStorage;
import io.github.dimkich.integration.testing.postgresql.PostgresDatabase;
import io.github.dimkich.integration.testing.postgresql.model.revert.PostgresExactMutationImpactFactory;
import io.github.dimkich.integration.testing.postgresql.model.revert.PostgresMutationImpactFactory;
import io.github.dimkich.integration.testing.postgresql.repository.PostgresCompositeTypeRepository;
import io.github.dimkich.integration.testing.postgresql.repository.PostgresJournalRepository;
import io.github.dimkich.integration.testing.postgresql.repository.PostgresTableCatalogRepository;
import io.github.dimkich.integration.testing.postgresql.repository.PostgresTableMetadataRepository;
import io.github.dimkich.integration.testing.postgresql.repository.PostgresTypeCatalogRepository;
import io.github.dimkich.integration.testing.postgresql.serde.PostgresColumnSerdeResolver;
import io.github.dimkich.integration.testing.postgresql.serde.PostgresFieldSerdeFactory;
import io.github.dimkich.integration.testing.postgresql.service.PostgresTableMetadataService;
import io.github.dimkich.integration.testing.postgresql.service.change.PostgresChangePipeline;
import io.github.dimkich.integration.testing.postgresql.service.change.PostgresMutationAssembler;
import io.github.dimkich.integration.testing.postgresql.service.diff.PostgresDiffService;
import io.github.dimkich.integration.testing.postgresql.service.journal.PostgresJournalLifecycle;
import io.github.dimkich.integration.testing.postgresql.service.journal.PostgresJournalReader;
import io.github.dimkich.integration.testing.serde.SerdeManager;
import io.github.dimkich.integration.testing.serde.impl.JacksonDeserializer;
import io.github.dimkich.integration.testing.storage.ObjectsDifference;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionProcessor;
import io.github.dimkich.integration.testing.storage.mapping.StorageMappingConfig;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import io.github.dimkich.integration.testing.storage.sql.SequenceResetScheduler;
import io.github.dimkich.integration.testing.storage.sql.StorageStateFactory;
import io.github.dimkich.integration.testing.storage.sql.StorageTransitionExecutor;
import io.github.dimkich.integration.testing.storage.sql.TableRevisionTracker;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import lombok.Setter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * Spring configuration for the trigger based PostgreSQL delta storage.
 * <p>
 * Imports {@link StorageMappingConfig} and, for every connection declared under
 * {@code integration.testing.postgresql.connections}, registers its components and a
 * {@link SQLDataStorageService} bean (named {@code <connection>TestDataStorage} to avoid
 * clashing with the application {@code DataSource} bean). Connections are read straight from the
 * {@link Environment} because {@link PostgresProperties} is not bound yet when the post processor
 * runs. The factory methods used by the per-connection definitions live in
 * {@link PostgresBeanFactory}.
 */
@Configuration
@EnableConfigurationProperties(PostgresProperties.class)
@Import({
        StorageMappingConfig.class,
        PostgresFieldSerdeFactory.class,
        PostgresBeanFactory.class,
        PostgresConfig.PostProcessor.class
})
public class PostgresConfig {

    @Bean
    public JacksonDeserializer postgresFieldJsonDeserializer(
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new).copy()
                .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
        return new JacksonDeserializer(mapper, Object.class);
    }

    /**
     * Registers connection specific beans before the context is refreshed.
     * <p>
     * The registry phase is late enough to see all bean definitions, so the shared beans can be
     * resolved by type. The connection factory methods are provided by {@link PostgresBeanFactory},
     * whose imported bean name is its fully qualified class name.
     */
    @Setter
    public static class PostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {
        private Environment environment;

        @Override
        public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws BeansException {
            Map<String, Object> connections = Binder.get(environment)
                    .bind("integration.testing.postgresql.connections", Bindable.mapOf(String.class, Object.class))
                    .orElse(Map.of());
            if (connections.isEmpty()) {
                return;
            }
            ListableBeanFactory beanFactory = (ListableBeanFactory) registry;
            String objectsDifference = singleBeanName(beanFactory, ObjectsDifference.class);
            String fieldExclusionProcessor = singleBeanName(beanFactory, FieldExclusionProcessor.class);
            String dateTimeService = singleBeanName(beanFactory, DateTimeService.class);
            String serdeManager = singleBeanName(beanFactory, SerdeManager.class);
            for (String name : connections.keySet()) {
                registerConnectionBeans(registry, name, objectsDifference,
                        fieldExclusionProcessor, dateTimeService, serdeManager);
            }
        }

        @Override
        public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        }

        private void registerConnectionBeans(BeanDefinitionRegistry registry, String name,
                                             String objectsDifference, String fieldExclusionProcessor,
                                             String dateTimeService, String serdeManager) {
            BeanNames beans = new BeanNames(name);
            registerRepositories(registry, name, beans, dateTimeService, fieldExclusionProcessor);
            registerServices(registry, name, beans, objectsDifference, serdeManager);
            registerStorage(registry, name, beans);
        }

        private void registerRepositories(BeanDefinitionRegistry registry, String name, BeanNames beans,
                                          String dateTimeService, String fieldExclusionProcessor) {
            registerIfAbsent(registry, beans.connectionProperties,
                    BeanDefinitionBuilder.rootBeanDefinition(PostgresProperties.Connection.class)
                            .setFactoryMethodOnBean("createConnectionProperties", beans.postgresBeanFactory)
                            .addConstructorArgValue(name)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.database,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresDatabase.class)
                            .addConstructorArgReference(beans.connectionProperties)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.tableCatalogRepository,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresTableCatalogRepository.class)
                            .addConstructorArgReference(beans.database)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.tableMetadataRepository,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresTableMetadataRepository.class)
                            .addConstructorArgReference(beans.database)
                            .addConstructorArgReference(beans.typeCatalogRepository)
                            .addConstructorArgReference(beans.connectionProperties)
                            .addConstructorArgReference(beans.columnSerdeResolver)
                            .addConstructorArgReference(dateTimeService)
                            .addConstructorArgReference(fieldExclusionProcessor)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.compositeTypeRepository,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresCompositeTypeRepository.class)
                            .addConstructorArgReference(beans.database)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.typeCatalogRepository,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresTypeCatalogRepository.class)
                            .addConstructorArgReference(beans.database)
                            .addConstructorArgReference(beans.compositeTypeRepository)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.journalRepository,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresJournalRepository.class)
                            .addConstructorArgReference(beans.database)
                            .getBeanDefinition());
        }

        private void registerServices(BeanDefinitionRegistry registry, String name, BeanNames beans,
                                      String objectsDifference, String serdeManager) {
            registerIfAbsent(registry, beans.journalLifecycle,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresJournalLifecycle.class)
                            .addConstructorArgReference(beans.journalRepository)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.journalReader,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresJournalReader.class)
                            .addConstructorArgReference(beans.journalLifecycle)
                            .addConstructorArgReference(beans.journalRepository)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.columnSerdeResolver,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresColumnSerdeResolver.class)
                            .addConstructorArgReference(serdeManager)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.tableMetadataService,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresTableMetadataService.class)
                            .addConstructorArgReference(beans.tableMetadataRepository)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.diffService,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresDiffService.class)
                            .addConstructorArgValue(name)
                            .addConstructorArgReference(objectsDifference)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.exactMutationImpactFactory,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresExactMutationImpactFactory.class)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.mutationImpactFactory,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresMutationImpactFactory.class)
                            .addConstructorArgReference(beans.exactMutationImpactFactory)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.mutationAssembler,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresMutationAssembler.class)
                            .addConstructorArgReference(beans.mutationImpactFactory)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.changePipeline,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresChangePipeline.class)
                            .addConstructorArgReference(beans.journalReader)
                            .addConstructorArgReference(beans.diffService)
                            .addConstructorArgReference(beans.mutationAssembler)
                            .getBeanDefinition());
        }

        private void registerStorage(BeanDefinitionRegistry registry, String name, BeanNames beans) {
            registerIfAbsent(registry, beans.tableRegistry,
                    BeanDefinitionBuilder.rootBeanDefinition(TableRegistry.class)
                            .setFactoryMethodOnBean("createTableRegistry", beans.postgresBeanFactory)
                            .addConstructorArgReference(beans.connectionProperties)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.dataStorage,
                    BeanDefinitionBuilder.genericBeanDefinition(PostgresDataStorage.class)
                            .addConstructorArgValue(name)
                            .addConstructorArgReference(beans.tableRegistry)
                            .addConstructorArgReference(beans.database)
                            .addConstructorArgReference(beans.tableCatalogRepository)
                            .addConstructorArgReference(beans.typeCatalogRepository)
                            .addConstructorArgReference(beans.journalLifecycle)
                            .addConstructorArgReference(beans.tableMetadataService)
                            .addConstructorArgReference(beans.changePipeline)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.fixture,
                    BeanDefinitionBuilder.genericBeanDefinition(DbUnitFixture.class)
                            .addConstructorArgReference(beans.tableRegistry)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.storageStateFactory,
                    BeanDefinitionBuilder.genericBeanDefinition(StorageStateFactory.class)
                            .addConstructorArgReference(beans.tableRegistry)
                            .addConstructorArgReference(beans.fixture)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.transitionExecutor,
                    BeanDefinitionBuilder.genericBeanDefinition(StorageTransitionExecutor.class)
                            .addConstructorArgReference(beans.dataStorage)
                            .addConstructorArgReference(beans.fixture)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.revisionTracker,
                    BeanDefinitionBuilder.genericBeanDefinition(TableRevisionTracker.class)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.sequenceResetScheduler,
                    BeanDefinitionBuilder.genericBeanDefinition(SequenceResetScheduler.class)
                            .addConstructorArgReference(beans.dataStorage)
                            .getBeanDefinition());
            registerIfAbsent(registry, beans.service,
                    BeanDefinitionBuilder.rootBeanDefinition(SQLDataStorageService.class)
                            .setFactoryMethodOnBean("createDataStorageService", beans.postgresBeanFactory)
                            .addConstructorArgReference(beans.dataStorage)
                            .addConstructorArgReference(beans.tableRegistry)
                            .addConstructorArgReference(beans.fixture)
                            .addConstructorArgReference(beans.storageStateFactory)
                            .addConstructorArgReference(beans.transitionExecutor)
                            .addConstructorArgReference(beans.revisionTracker)
                            .addConstructorArgReference(beans.sequenceResetScheduler)
                            .getBeanDefinition());
        }

        private String singleBeanName(ListableBeanFactory beanFactory, Class<?> type) {
            String[] names = beanFactory.getBeanNamesForType(type, true, false);
            if (names.length == 0) {
                throw new IllegalStateException("Bean of type " + type.getName() + " is not registered");
            }
            if (names.length > 1) {
                throw new IllegalStateException("Multiple beans of type " + type.getName()
                        + " are registered: " + String.join(", ", names));
            }
            return names[0];
        }

        private void registerIfAbsent(BeanDefinitionRegistry registry, String name,
                                      AbstractBeanDefinition definition) {
            if (!registry.containsBeanDefinition(name)) {
                registry.registerBeanDefinition(name, definition);
            }
        }
    }

    private static final class BeanNames {
        private final String postgresBeanFactory;
        private final String connectionProperties;
        private final String database;
        private final String tableCatalogRepository;
        private final String tableMetadataRepository;
        private final String compositeTypeRepository;
        private final String typeCatalogRepository;
        private final String journalRepository;
        private final String journalLifecycle;
        private final String journalReader;
        private final String columnSerdeResolver;
        private final String tableMetadataService;
        private final String diffService;
        private final String exactMutationImpactFactory;
        private final String mutationImpactFactory;
        private final String mutationAssembler;
        private final String changePipeline;
        private final String dataStorage;
        private final String tableRegistry;
        private final String fixture;
        private final String storageStateFactory;
        private final String transitionExecutor;
        private final String revisionTracker;
        private final String sequenceResetScheduler;
        private final String service;

        BeanNames(String name) {
            this.postgresBeanFactory = PostgresBeanFactory.class.getName();
            this.connectionProperties = name + "PostgresConnectionProperties";
            this.database = name + "PostgresDatabase";
            this.tableCatalogRepository = name + "PostgresTableCatalogRepository";
            this.tableMetadataRepository = name + "PostgresTableMetadataRepository";
            this.compositeTypeRepository = name + "PostgresCompositeTypeRepository";
            this.typeCatalogRepository = name + "PostgresTypeCatalogRepository";
            this.journalRepository = name + "PostgresJournalRepository";
            this.journalLifecycle = name + "PostgresJournalLifecycle";
            this.journalReader = name + "PostgresJournalReader";
            this.columnSerdeResolver = name + "PostgresColumnSerdeResolver";
            this.tableMetadataService = name + "PostgresTableMetadataService";
            this.diffService = name + "PostgresDiffService";
            this.exactMutationImpactFactory = name + "PostgresExactMutationImpactFactory";
            this.mutationImpactFactory = name + "PostgresMutationImpactFactory";
            this.mutationAssembler = name + "PostgresMutationAssembler";
            this.changePipeline = name + "PostgresChangePipeline";
            this.dataStorage = name + "PostgresDataStorage";
            this.tableRegistry = name + "TableRegistry";
            this.fixture = name + "DbUnitFixture";
            this.storageStateFactory = name + "StorageStateFactory";
            this.transitionExecutor = name + "StorageTransitionExecutor";
            this.revisionTracker = name + "TableRevisionTracker";
            this.sequenceResetScheduler = name + "SequenceResetScheduler";
            this.service = name + "TestDataStorage";
        }
    }
}

package io.github.dimkich.integration.testing.redis.config;

import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.redis.RedisTestDataStorage;
import io.github.dimkich.integration.testing.redis.accessor.*;
import io.github.dimkich.integration.testing.redis.codec.RedissonCodecAdapter;
import io.github.dimkich.integration.testing.redis.codec.SpringDataCodecAdapter;
import io.github.dimkich.integration.testing.redis.codec.segment.*;
import io.github.dimkich.integration.testing.redis.model.*;
import io.github.dimkich.integration.testing.redis.registry.BinaryEnvelopeService;
import io.github.dimkich.integration.testing.redis.registry.RedisAdapterResolver;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.redis.registry.RedisObjectFactory;
import io.github.dimkich.integration.testing.redis.replication.*;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSyncStateDelegator;
import io.github.dimkich.integration.testing.redis.replication.event.listener.UnknownPhaseListener;
import io.github.dimkich.integration.testing.redis.schema.RedissonSchemaAdapter;
import io.github.dimkich.integration.testing.redis.schema.SpringDataSchemaAdapter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.List;

/**
 * Spring configuration for Redis integration testing.
 * <p>
 * Imported by {@link io.github.dimkich.integration.testing.redis.EnableTestRedis}.
 * Registers codecs, schemas, accessors, replication handlers, and supporting infrastructure.
 * For each {@link RedisConnectionFactory} bean in the context, {@link PostProcessor} registers a
 * per-factory stack of sync barrier, connection settings, in-memory store, test data storage,
 * sync-state delegator, and replicator beans (see {@link BeanNames} for naming).
 *
 * @see RedisProperties
 * @see io.github.dimkich.integration.testing.redis.EnableTestRedis
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@ComponentScan(basePackages = {"io.github.dimkich.integration.testing.redis.replication.handler"})
@Import({RedisConfig.PostProcessor.class, RedisDataSchemaRegistry.class, RedisObjectFactory.class,
        RedisAccessorCoordinator.class, StringDataAccessor.class, HashDataAccessor.class, ListDataAccessor.class,
        SetDataAccessor.class, ZSetDataAccessor.class, StreamDataAccessor.class, RedissonCodecAdapter.class,
        SpringDataCodecAdapter.class, SpringDataSchemaAdapter.class, RedissonSchemaAdapter.class,
        RedisSyncManager.class, RedisReplicatorFactory.class, BinaryFormatParser.class, ContentSegmentProvider.class,
        LengthSegmentProvider.class, TsSegmentProvider.class, FixSegmentProvider.class, Crc32SegmentProvider.class,
        VersionSegmentProvider.class, StringSegmentProvider.class, HyperLogLogDataAccessor.class,
        UnknownPhaseListener.class, RedisAdapterResolver.class, BinaryEnvelopeService.class,
        RedisKeyResolver.class
})
public class RedisConfig {

    /**
     * Registers Redis model types with the integration-testing Jackson module for polymorphic
     * serialization in test fixtures.
     */
    @Bean
    TestSetupModule redisModule() {
        return new TestSetupModule().addParentType(RedisValue.class).addSubTypes(RedisHash.class, RedisList.class,
                RedisSet.class, RedisStream.class, RedisStreamEntry.class, RedisZSet.class, RedisEntry.class,
                RedisZSetEntry.class, RedisKey.class, RedisHyperLogLog.class);
    }

    /**
     * Dispatches replication snapshot events to all {@link RedisSnapshotHandler} beans.
     */
    @Bean
    public RedisEventDispatcher snapshotDispatcher(List<RedisSnapshotHandler<?>> handlers) {
        return new RedisEventDispatcher(handlers);
    }

    /**
     * Dispatches replication stream events to all {@link RedisStreamHandler} beans.
     */
    @Bean
    public RedisEventDispatcher streamDispatcher(List<RedisStreamHandler<?>> handlers) {
        return new RedisEventDispatcher(handlers);
    }

    /**
     * Registers per-{@link RedisConnectionFactory} beans before the context refreshes.
     * <p>
     * For each connection factory, creates definitions for sync barrier, in-memory store,
     * test data storage, sync-state delegator, and replicator.
     * The delegator subscribes itself to the replicator in {@code @PostConstruct}.
     */
    public static class PostProcessor implements BeanDefinitionRegistryPostProcessor {

        /**
         * Discovers all {@link RedisConnectionFactory} beans and registers the derived bean
         * definitions named by {@link BeanNames}.
         */
        @Override
        public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws BeansException {
            ListableBeanFactory listableBeanFactory = (ListableBeanFactory) registry;
            String propertiesName = listableBeanFactory
                    .getBeanNamesForType(RedisProperties.class, true, false)[0];
            String[] factoryNames = listableBeanFactory
                    .getBeanNamesForType(RedisConnectionFactory.class, true, false);

            for (String factoryName : factoryNames) {
                BeanNames beans = new BeanNames(factoryName, propertiesName);
                registry.registerBeanDefinition(beans.barrier, createBarrierDef(beans));
                registry.registerBeanDefinition(beans.memStore, createMemStoreDef(beans));
                registry.registerBeanDefinition(beans.storage, createStorageDef(beans));
                registry.registerBeanDefinition(beans.delegator, createDelegatorDef(beans));
                registry.registerBeanDefinition(beans.replicator, createReplicatorDef(beans));
            }
        }

        /** Bean definition for {@link RedisSyncBarrier} tied to {@code beans.factory}. */
        private BeanDefinition createBarrierDef(BeanNames beans) {
            return BeanDefinitionBuilder.genericBeanDefinition(RedisSyncBarrier.class)
                    .addConstructorArgValue(beans.factory)
                    .addConstructorArgReference(beans.factory)
                    .getBeanDefinition();
        }

        /** Bean definition for {@link RedisInMemoryStore}. */
        private BeanDefinition createMemStoreDef(BeanNames beans) {
            AbstractBeanDefinition def = BeanDefinitionBuilder
                    .genericBeanDefinition(RedisInMemoryStore.class)
                    .getBeanDefinition();
            def.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            def.getConstructorArgumentValues().addIndexedArgumentValue(0, beans.factory);
            def.getConstructorArgumentValues().addIndexedArgumentValue(1, new RuntimeBeanReference(beans.barrier));
            def.getConstructorArgumentValues().addIndexedArgumentValue(2, new RuntimeBeanReference(beans.properties));
            def.getConstructorArgumentValues().addIndexedArgumentValue(3, new RuntimeBeanReference(beans.replicator));
            return def;
        }

        /** Bean definition for {@link RedisTestDataStorage}. */
        private BeanDefinition createStorageDef(BeanNames beans) {
            AbstractBeanDefinition def = BeanDefinitionBuilder
                    .genericBeanDefinition(RedisTestDataStorage.class)
                    .getBeanDefinition();
            def.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            def.getConstructorArgumentValues().addIndexedArgumentValue(0, beans.factory);
            def.getConstructorArgumentValues().addIndexedArgumentValue(1, new RuntimeBeanReference(beans.factory));
            def.getConstructorArgumentValues().addIndexedArgumentValue(2, new RuntimeBeanReference(beans.memStore));
            return def;
        }

        /** Bean definition for {@link RedisSyncStateDelegator}. */
        private BeanDefinition createDelegatorDef(BeanNames beans) {
            AbstractBeanDefinition def = BeanDefinitionBuilder
                    .genericBeanDefinition(RedisSyncStateDelegator.class)
                    .getBeanDefinition();
            def.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            def.getConstructorArgumentValues().addIndexedArgumentValue(0, new RuntimeBeanReference(beans.memStore));
            def.getConstructorArgumentValues().addIndexedArgumentValue(1, new RuntimeBeanReference(beans.barrier));
            return def;
        }

        /**
         * Bean definition for the replicator created by {@link RedisReplicatorFactory}.
         */
        private BeanDefinition createReplicatorDef(BeanNames beans) {
            return BeanDefinitionBuilder.rootBeanDefinition(RedisReplicatorFactory.class)
                    .setFactoryMethodOnBean("createReplicator", beans.replicatorFactory)
                    .addConstructorArgValue(beans.factory)
                    .addConstructorArgReference(beans.factory)
                    .addConstructorArgReference(beans.barrier)
                    .getBeanDefinition();
        }

        @Override
        public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        }
    }

    /**
     * Derived Spring bean names for a single {@link RedisConnectionFactory}.
     * <p>
     * Each name is the factory bean name suffixed with a role-specific suffix, for example
     * {@code myRedisConnectionFactoryTestDataStorage}.
     */
    static class BeanNames {
        /** Name of the {@link RedisConnectionFactory} bean. */
        final String factory;
        /** Name of the {@link RedisProperties} bean. */
        final String properties;
        /** Sync barrier bean name ({@code factory + "SyncBarrier"}). */
        final String barrier;
        /** In-memory store bean name ({@code factory + "InMemoryStore"}). */
        final String memStore;
        /** Test data storage bean name ({@code factory + "TestDataStorage"}). */
        final String storage;
        /** Replicator bean name ({@code factory + "Replicator"}). */
        final String replicator;
        /** Sync-state delegator bean name ({@code factory + "SyncStateDelegator"}). */
        final String delegator;
        /** Fully qualified name of {@link RedisReplicatorFactory} for factory-method lookup. */
        final String replicatorFactory;

        /**
         * @param factoryName    {@link RedisConnectionFactory} bean name
         * @param propertiesName {@link RedisProperties} bean name
         */
        BeanNames(String factoryName, String propertiesName) {
            this.factory = factoryName;
            this.properties = propertiesName;
            this.barrier = factoryName + "SyncBarrier";
            this.memStore = factoryName + "InMemoryStore";
            this.storage = factoryName + "TestDataStorage";
            this.replicator = factoryName + "Replicator";
            this.delegator = factoryName + "SyncStateDelegator";
            this.replicatorFactory = RedisReplicatorFactory.class.getName();
        }
    }
}

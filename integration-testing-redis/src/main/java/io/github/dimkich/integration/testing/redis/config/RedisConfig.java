package io.github.dimkich.integration.testing.redis.config;

import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.message.TestMessages;
import io.github.dimkich.integration.testing.redis.RedisTestDataStorage;
import io.github.dimkich.integration.testing.redis.RedisWaitCompletion;
import io.github.dimkich.integration.testing.redis.accessor.*;
import io.github.dimkich.integration.testing.redis.client.RedisClient;
import io.github.dimkich.integration.testing.redis.client.Resp2RedisClient;
import io.github.dimkich.integration.testing.redis.message.RedisPush;
import io.github.dimkich.integration.testing.redis.message.RedisPushCapture;
import io.github.dimkich.integration.testing.redis.message.RedisPushSender;
import io.github.dimkich.integration.testing.redis.model.*;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.redis.registry.RedisObjectFactory;
import io.github.dimkich.integration.testing.redis.replication.*;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSyncStateDelegator;
import io.github.dimkich.integration.testing.redis.replication.event.listener.UnknownPhaseListener;
import io.github.dimkich.integration.testing.redis.serde.RedisSchemaSerdeFactory;
import io.github.dimkich.integration.testing.redis.serde.adapter.RedisRedissonDeserializerAdapter;
import io.github.dimkich.integration.testing.redis.serde.adapter.RedisRedissonSerializerAdapter;
import io.github.dimkich.integration.testing.redis.serde.adapter.RedisSpringDataDeserializerAdapter;
import io.github.dimkich.integration.testing.redis.serde.adapter.RedisSpringDataSerializerAdapter;
import lombok.NonNull;
import lombok.Setter;
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
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Spring configuration for Redis integration testing.
 * <p>
 * Imported by {@link io.github.dimkich.integration.testing.redis.EnableTestRedis}.
 * Registers codecs, schemas, accessors, replication handlers, and supporting infrastructure.
 * <p>
 * Connections are declared under {@code integration.testing.redis.connections}; the framework
 * opens its own connection for each entry instead of borrowing a
 * {@code RedisConnectionFactory} bean from the application. This makes the module independent
 * of the Redis driver used by the application under test. For every configured connection,
 * {@link PostProcessor} registers a per-connection stack: client, sync state, sync barrier,
 * in-memory store, test data storage, sync-state delegator, replicator, push sender and
 * wait-completion beans (see {@link BeanNames} for naming).
 *
 * @see RedisProperties
 * @see io.github.dimkich.integration.testing.redis.EnableTestRedis
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@ComponentScan(basePackages = {"io.github.dimkich.integration.testing.redis.replication.handler"})
@Import({RedisConfig.PostProcessor.class, RedisDataSchemaRegistry.class, RedisObjectFactory.class,
        RedisSchemaSerdeFactory.class,
        RedisAccessorCoordinator.class, StringDataAccessor.class, HashDataAccessor.class, ListDataAccessor.class,
        SetDataAccessor.class, ZSetDataAccessor.class, StreamDataAccessor.class,
        RedisSpringDataSerializerAdapter.class, RedisSpringDataDeserializerAdapter.class,
        RedisRedissonSerializerAdapter.class,
        RedisRedissonDeserializerAdapter.class,
        RedisSyncManager.class, RedisReplicatorFactory.class, HyperLogLogDataAccessor.class,
        UnknownPhaseListener.class, RedisKeyResolver.class
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
                RedisZSetEntry.class, RedisKey.class, RedisHyperLogLog.class, RedisPush.class);
    }

    /**
     * Captures Pub/Sub pushes observed on the replication stream for all connections and
     * puts them into the test message store.
     */
    @Bean
    public RedisPushCapture redisPushCapture(RedisDataSchemaRegistry redisDataSchemaRegistry,
                                             TestMessages testMessages) {
        return new RedisPushCapture(redisDataSchemaRegistry, testMessages);
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
     * Creates the framework client for a configured connection.
     *
     * @param name       logical connection name
     * @param properties Redis module properties
     * @return client bound to the connection settings
     */
    public static RedisClient createClient(String name, RedisProperties properties) {
        return new Resp2RedisClient(name, properties);
    }

    /**
     * Registers per-connection beans before the context refreshes.
     * <p>
     * Connection names are read from {@code integration.testing.redis.connections}. When the
     * section is empty, no per-connection beans are registered: Redis connections must be
     * declared explicitly, like in the PostgreSQL and Kafka modules.
     */
    @Setter
    public static class PostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {
        private Environment environment;

        /**
         * Binds connection names and registers the derived bean definitions named by
         * {@link BeanNames}.
         */
        @Override
        public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws BeansException {
            Set<String> connectionNames = bindConnectionNames();
            if (connectionNames.isEmpty()) {
                return;
            }
            String propertiesName = findPropertiesBeanName((ListableBeanFactory) registry);

            for (String connectionName : connectionNames) {
                BeanNames beans = new BeanNames(connectionName, propertiesName);
                register(registry, beans.client, createClientDef(beans));
                register(registry, beans.syncState, createSyncStateDef(beans));
                register(registry, beans.barrier, createBarrierDef(beans));
                register(registry, beans.memStore, createMemStoreDef(beans));
                register(registry, beans.storage, createStorageDef(beans));
                register(registry, beans.delegator, createDelegatorDef(beans));
                register(registry, beans.replicator, createReplicatorDef(beans));
                register(registry, beans.pushSender, createPushSenderDef(beans));
                register(registry, beans.waitCompletion, createWaitCompletionDef(beans));
            }
        }

        /**
         * Registers a derived bean unless the application already defined a bean with that name;
         * an explicit definition always wins and is never silently replaced.
         */
        private void register(BeanDefinitionRegistry registry, String name, BeanDefinition definition) {
            if (!registry.containsBeanDefinition(name)) {
                registry.registerBeanDefinition(name, definition);
            }
        }

        private String findPropertiesBeanName(ListableBeanFactory beanFactory) {
            String[] names = beanFactory.getBeanNamesForType(RedisProperties.class, true, false);
            if (names.length == 0) {
                throw new IllegalStateException("Bean of type " + RedisProperties.class.getName() + " is not registered");
            }
            return names[0];
        }

        private Set<String> bindConnectionNames() {
            Map<String, Object> connections = Binder.get(environment)
                    .bind("integration.testing.redis.connections", Bindable.mapOf(String.class, Object.class))
                    .orElseGet(Map::of);
            return new LinkedHashSet<>(connections.keySet());
        }

        private BeanDefinition createClientDef(BeanNames beans) {
            AbstractBeanDefinition def = BeanDefinitionBuilder.rootBeanDefinition(RedisConfig.class)
                    .setFactoryMethod("createClient")
                    .addConstructorArgValue(beans.name)
                    .addConstructorArgReference(beans.properties)
                    .getBeanDefinition();
            def.setDestroyMethodName("close");
            return def;
        }

        private BeanDefinition createSyncStateDef(BeanNames beans) {
            return BeanDefinitionBuilder.genericBeanDefinition(RedisSyncState.class)
                    .addConstructorArgValue(beans.name)
                    .getBeanDefinition();
        }

        private BeanDefinition createBarrierDef(BeanNames beans) {
            return BeanDefinitionBuilder.genericBeanDefinition(RedisSyncBarrier.class)
                    .addConstructorArgValue(beans.name)
                    .addConstructorArgReference(beans.client)
                    .addConstructorArgReference(beans.syncState)
                    .getBeanDefinition();
        }

        private BeanDefinition createMemStoreDef(BeanNames beans) {
            AbstractBeanDefinition def = BeanDefinitionBuilder
                    .genericBeanDefinition(RedisInMemoryStore.class)
                    .getBeanDefinition();
            def.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            def.getConstructorArgumentValues().addIndexedArgumentValue(0, beans.name);
            def.getConstructorArgumentValues().addIndexedArgumentValue(1, new RuntimeBeanReference(beans.barrier));
            def.getConstructorArgumentValues().addIndexedArgumentValue(2, new RuntimeBeanReference(beans.replicator));
            return def;
        }

        private BeanDefinition createStorageDef(BeanNames beans) {
            AbstractBeanDefinition def = BeanDefinitionBuilder
                    .genericBeanDefinition(RedisTestDataStorage.class)
                    .getBeanDefinition();
            def.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            def.getConstructorArgumentValues().addIndexedArgumentValue(0, beans.name);
            def.getConstructorArgumentValues().addIndexedArgumentValue(1, new RuntimeBeanReference(beans.client));
            def.getConstructorArgumentValues().addIndexedArgumentValue(2, new RuntimeBeanReference(beans.memStore));
            return def;
        }

        private BeanDefinition createDelegatorDef(BeanNames beans) {
            AbstractBeanDefinition def = BeanDefinitionBuilder
                    .genericBeanDefinition(RedisSyncStateDelegator.class)
                    .getBeanDefinition();
            def.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            def.getConstructorArgumentValues().addIndexedArgumentValue(0, new RuntimeBeanReference(beans.memStore));
            def.getConstructorArgumentValues().addIndexedArgumentValue(1, new RuntimeBeanReference(beans.syncState));
            return def;
        }

        private BeanDefinition createReplicatorDef(BeanNames beans) {
            return BeanDefinitionBuilder.rootBeanDefinition(RedisReplicatorFactory.class)
                    .setFactoryMethodOnBean("createReplicator", beans.replicatorFactory)
                    .addConstructorArgValue(beans.name)
                    .addConstructorArgReference(beans.client)
                    .addConstructorArgReference(beans.syncState)
                    .getBeanDefinition();
        }

        private BeanDefinition createPushSenderDef(BeanNames beans) {
            AbstractBeanDefinition def = BeanDefinitionBuilder
                    .genericBeanDefinition(RedisPushSender.class)
                    .addConstructorArgValue(beans.name)
                    .addConstructorArgReference(beans.client)
                    .getBeanDefinition();
            def.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            return def;
        }

        private BeanDefinition createWaitCompletionDef(BeanNames beans) {
            AbstractBeanDefinition def = BeanDefinitionBuilder
                    .genericBeanDefinition(RedisWaitCompletion.class)
                    .addConstructorArgReference(beans.syncState)
                    .addConstructorArgReference(beans.barrier)
                    .getBeanDefinition();
            def.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            return def;
        }

        /** No-op: every bean definition is registered in {@link #postProcessBeanDefinitionRegistry}. */
        @Override
        public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        }
    }

    /**
     * Derived Spring bean names for a single configured Redis connection.
     * <p>
     * Each name is the logical connection name suffixed with a role-specific suffix, for example
     * {@code myRedisClient} or {@code myRedisTestDataStorage}.
     */
    static class BeanNames {
        /** Logical connection name configured under {@code integration.testing.redis.connections}. */
        final String name;
        /** Name of the {@link RedisProperties} bean. */
        final String properties;
        /** Framework client bean name ({@code name + "RedisClient"}). */
        final String client;
        /** Sync state bean name ({@code name + "SyncState"}). */
        final String syncState;
        /** Sync barrier bean name ({@code name + "SyncBarrier"}). */
        final String barrier;
        /** In-memory store bean name ({@code name + "InMemoryStore"}). */
        final String memStore;
        /** Test data storage bean name ({@code name + "TestDataStorage"}). */
        final String storage;
        /** Replicator bean name ({@code name + "Replicator"}). */
        final String replicator;
        /** Sync-state delegator bean name ({@code name + "SyncStateDelegator"}). */
        final String delegator;
        /** Pub/Sub push sender bean name ({@code name + "RedisPushSender"}). */
        final String pushSender;
        /** Wait-completion bean name ({@code name + "RedisWaitCompletion"}). */
        final String waitCompletion;
        /** Fully qualified name of {@link RedisReplicatorFactory} for factory-method lookup. */
        final String replicatorFactory;

        /**
         * @param name           logical connection name
         * @param propertiesName {@link RedisProperties} bean name
         */
        BeanNames(String name, String propertiesName) {
            this.name = name;
            this.properties = propertiesName;
            this.client = name + "RedisClient";
            this.syncState = name + "SyncState";
            this.barrier = name + "SyncBarrier";
            this.memStore = name + "InMemoryStore";
            this.storage = name + "TestDataStorage";
            this.replicator = name + "Replicator";
            this.delegator = name + "SyncStateDelegator";
            this.pushSender = name + "RedisPushSender";
            this.waitCompletion = name + "RedisWaitCompletion";
            this.replicatorFactory = RedisReplicatorFactory.class.getName();
        }
    }
}

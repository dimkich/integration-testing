package io.github.dimkich.integration.testing.kafka.config;

import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.instrumentation.InstrumentationManager;
import io.github.dimkich.integration.testing.kafka.*;
import io.github.dimkich.integration.testing.kafka.inflight.agent.KafkaInFlightAgent;
import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedger;
import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedgerScope;
import io.github.dimkich.integration.testing.kafka.registry.KafkaObjectFactory;
import io.github.dimkich.integration.testing.kafka.registry.KafkaTopicRegistry;
import io.github.dimkich.integration.testing.kafka.serde.KafkaRecordSerdeFactory;
import io.github.dimkich.integration.testing.kafka.serde.adapter.*;
import io.github.dimkich.integration.testing.kafka.serde.provider.SpringJsonKafkaSerdeProvider;
import io.github.dimkich.integration.testing.kafka.serde.provider.SpringXmlKafkaSerdeProvider;
import io.github.dimkich.integration.testing.kafka.util.BootstrapUtil;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
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
import org.springframework.kafka.support.DefaultKafkaHeaderMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring configuration of the Kafka module: registers the serde factory, topic
 * registry, sniffer manager and serde adapters, and creates per-connection
 * infrastructure beans (producers, consumers, admin clients, sniffers, wait
 * completion and message senders) through {@link PostProcessor}.
 */
@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
@Import({
        KafkaRecordSerdeFactory.class,
        KafkaObjectFactory.class,
        KafkaTopicRegistry.class,
        KafkaSnifferManager.class,

        SpringJsonKafkaSerdeProvider.class,
        SpringXmlKafkaSerdeProvider.class,

        CoreToKafkaSerializerAdapter.class,
        CoreToKafkaDeserializerAdapter.class,

        TestSerdeSerializerToKafkaRecordSerializerAdapter.class,
        TestSerdeDeserializerToKafkaRecordDeserializerAdapter.class,

        KafkaSerializerToKafkaRecordSerializerAdapter.class,
        KafkaDeserializerToKafkaRecordDeserializerAdapter.class,

        KafkaHeaderMapperToKafkaHeaderSerializerAdapter.class,
        KafkaHeaderMapperToKafkaHeaderDeserializerAdapter.class,
        TestSerdeSerializerToKafkaHeaderSerializerAdapter.class,
        TestSerdeDeserializerToKafkaHeaderDeserializerAdapter.class,
        KafkaSerializerToKafkaHeaderSerializerAdapter.class,
        KafkaDeserializerToKafkaHeaderDeserializerAdapter.class,

        KafkaConfig.PostProcessor.class
})
@RequiredArgsConstructor
public class KafkaConfig {
    @Bean
    TestSetupModule kafkaModule() {
        return new TestSetupModule().addSubTypes(KafkaRecord.class);
    }

    @Bean
    DefaultKafkaHeaderMapper kafkaHeaderMapper() {
        return new DefaultKafkaHeaderMapper();
    }

    static KafkaProducer<byte[], byte[]> createProducer(ConnectionProperties connectionProperties) {
        Properties props = new Properties();
        props.putAll(connectionProperties.toClientProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        return new KafkaProducer<>(props);
    }

    static KafkaConsumer<byte[], byte[]> createConsumer(ConnectionProperties connectionProperties) {
        Properties props = new Properties();
        props.putAll(connectionProperties.toClientProperties());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-sniffer-" + UUID.randomUUID());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        ByteArrayDeserializer byteArrayDeserializer = new ByteArrayDeserializer();
        return new KafkaConsumer<>(props, byteArrayDeserializer, byteArrayDeserializer);
    }

    static AdminClient createAdminClient(ConnectionProperties connectionProperties) {
        return AdminClient.create(connectionProperties.toClientProperties());
    }

    /**
     * Registers per-connection Kafka infrastructure beans derived from the
     * {@code integration.testing.kafka.connections} configuration, grouping
     * connections that share both a cluster address and client properties.
     */
    public static class PostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {
        private Environment environment;

        @Override
        @SuppressWarnings("NullableProblems")
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            String propertiesName = findPropertiesBeanName(registry);
            Map<String, ConnectionBootstrap> connectionKeys = bindConnectionKeys();
            Map<ClusterKey, BeanNames> bootstrapGroups = buildBootstrapGroups(connectionKeys);

            boolean inflight = KafkaInFlightAgent.isInflightActive();
            assertInstrumentationInstalled();

            for (BeanNames group : bootstrapGroups.values()) {
                registry.registerBeanDefinition(group.adminClient, createAdminClientDef(group));
                registry.registerBeanDefinition(group.adminRepo, createAdminRepoDef(group.adminClient));
                registry.registerBeanDefinition(group.connectionProperties, createConnectionDef(group, propertiesName));
                registry.registerBeanDefinition(group.producer, createBootstrapProducerDef(group));

                String stateCheckerRef;
                if (inflight) {
                    stateCheckerRef = group.stateChecker;
                    registry.registerBeanDefinition(stateCheckerRef, createStateCheckerDef(group.rawBootstrap));
                } else {
                    stateCheckerRef = group.adminRepo;
                }

                registry.registerBeanDefinition(group.snifferConsumer, createBootstrapConsumerDef(group));
                registry.registerBeanDefinition(group.snifferRebalanceListener,
                        createBootstrapRebalanceListenerDef(group));
                registry.registerBeanDefinition(group.sniffer, createBootstrapSnifferDef(group));
                registry.registerBeanDefinition(group.waitCompletion,
                        createWaitCompletionDef(group.sniffer, stateCheckerRef));
                registry.registerBeanDefinition(group.messageSender, createBootstrapSenderDef(group.producer, group.names));
            }

            registry.registerBeanDefinition("inboundMessageRegistry",
                    BeanDefinitionBuilder.genericBeanDefinition(InboundMessageRegistry.class).getBeanDefinition());

            if (inflight) {
                // InFlightLedger keys clusters by normalized bootstrap only.
                // Multiple groups on the same bootstrap (different client
                // properties) must share one cluster state, so the distinct
                // bootstraps are passed here instead of the group keys.
                List<String> uniqueBootstraps = bootstrapGroups.values().stream()
                        .map(b -> b.normalizedBootstrap)
                        .distinct()
                        .collect(Collectors.toList());

                registry.registerBeanDefinition("inFlightLedger",
                        BeanDefinitionBuilder.genericBeanDefinition(InFlightLedger.class)
                                .addConstructorArgValue(uniqueBootstraps)
                                .getBeanDefinition());
            }
        }

        private String findPropertiesBeanName(BeanDefinitionRegistry registry) {
            ListableBeanFactory listableBeanFactory = (ListableBeanFactory) registry;
            return listableBeanFactory.getBeanNamesForType(KafkaProperties.class, true, false)[0];
        }

        /**
         * Verifies that instrumentation has been installed before the Kafka context
         * is built.
         *
         * <p>The Kafka module always relies on at least one active
         * {@code InstrumentationPlugin}: {@code SugarCubesClonerPlugin} is applicable
         * to every test class, so a completed
         * {@link InstrumentationManager#install(Class)} never leaves the plugin list
         * empty. An empty list therefore means {@code install()} has not run yet —
         * typically because {@code SpringExtension.beforeAll} created the context
         * before {@code JunitExtension.beforeAll}. See the
         * {@code @Order(HIGHEST_PRECEDENCE)} annotation on
         * {@code JunitExtension}.
         *
         * <p>Failing here turns a silent misconfiguration (inflight mode disabled,
         * admin-based state checker used instead, no ByteBuddy transformer installed)
         * into an explicit startup error.
         */
        private void assertInstrumentationInstalled() {
            if (InstrumentationManager.getActivePlugins().isEmpty()) {
                throw new IllegalStateException(
                        "Kafka context is being built but no instrumentation plugins are active. "
                                + "This usually means JunitExtension.beforeAll has not run yet "
                                + "(check @Order on JunitExtension), or the test was started "
                                + "without @IntegrationTesting.");
            }
        }

        /**
         * Binds the connection map through the manual {@code Binder}.
         *
         * <p>The target type is {@link ConnectionBootstrap} rather than the full
         * {@link ConnectionProperties} on purpose: the manual binder does not see
         * {@code @ConfigurationPropertiesBinding} beans, so binding a type with
         * {@code java.lang.reflect.Type} fields (via {@code SerdeProperties.targetClass})
         * would fail with {@code ConverterNotFoundException}. Only the fields needed
         * for grouping are bound here; the full config is resolved later via
         * {@code KafkaProperties.getConnection(name)}.
         */
        private Map<String, ConnectionBootstrap> bindConnectionKeys() {
            return Binder.get(environment)
                    .bind("integration.testing.kafka.connections",
                            Bindable.mapOf(String.class, ConnectionBootstrap.class))
                    .orElseGet(Map::of);
        }

        private Map<ClusterKey, BeanNames> buildBootstrapGroups(Map<String, ConnectionBootstrap> connections) {
            Map<ClusterKey, BeanNames> groups = new LinkedHashMap<>();
            for (Map.Entry<String, ConnectionBootstrap> entry : connections.entrySet()) {
                ConnectionBootstrap conn = entry.getValue();
                String rawBootstrap = conn.getBootstrapServers();
                if (rawBootstrap == null) {
                    throw new IllegalStateException(
                            "Connection [" + entry.getKey() + "] must have bootstrapServers configured");
                }
                // Two connections share client beans only when both the cluster
                // address and the effective client properties are identical.
                // Otherwise, each connection needs its own
                // KafkaProducer/KafkaConsumer/AdminClient.
                ClusterKey key = new ClusterKey(BootstrapUtil.normalize(rawBootstrap), conn.getProperties());
                groups.computeIfAbsent(key, k -> new BeanNames(rawBootstrap, k.properties()))
                        .names.add(entry.getKey());
            }
            return groups;
        }

        /**
         * Grouping key for client-side beans. Combines the normalized bootstrap
         * address with the connection-level client properties so that two
         * connections to the same cluster but with different
         * {@code security.protocol}, {@code sasl.*}, {@code acks},
         * {@code client.id}, {@code transactional.id}, etc. receive separate
         * Kafka clients instead of silently sharing the first one's config.
         *
         * <p>Properties are defensively copied into an unmodifiable map to
         * guarantee stable {@code equals}/{@code hashCode} for the lifetime of
         * the key.
         */
        private record ClusterKey(String normalizedBootstrap, Map<String, Object> properties) {
            ClusterKey {
                properties = properties == null
                        ? Map.of()
                        : Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            }
        }

        private AbstractBeanDefinition createConnectionDef(BeanNames group, String propertiesName) {
            String firstConnName = group.names.get(0);
            return BeanDefinitionBuilder
                    .rootBeanDefinition(ConnectionProperties.class)
                    .setFactoryMethodOnBean("getConnection", propertiesName)
                    .addConstructorArgValue(firstConnName)
                    .getBeanDefinition();
        }

        private AbstractBeanDefinition createBootstrapProducerDef(BeanNames group) {
            AbstractBeanDefinition def = BeanDefinitionBuilder
                    .rootBeanDefinition(KafkaConfig.class)
                    .setFactoryMethod("createProducer")
                    .addConstructorArgReference(group.connectionProperties)
                    .getBeanDefinition();
            def.setDestroyMethodName("close");
            return def;
        }

        private AbstractBeanDefinition createBootstrapSenderDef(String producerRef, List<String> connectionNames) {
            List<String> namesList = new ArrayList<>(connectionNames);
            AbstractBeanDefinition def = BeanDefinitionBuilder
                    .genericBeanDefinition(KafkaMessageSender.class)
                    .addConstructorArgValue(namesList)
                    .addConstructorArgReference(producerRef)
                    .getBeanDefinition();
            def.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            return def;
        }

        private AbstractBeanDefinition createBootstrapConsumerDef(BeanNames group) {
            AbstractBeanDefinition def = BeanDefinitionBuilder
                    .rootBeanDefinition(KafkaConfig.class)
                    .setFactoryMethod("createConsumer")
                    .addConstructorArgReference(group.connectionProperties)
                    .getBeanDefinition();
            def.setDestroyMethodName("close");
            return def;
        }

        private AbstractBeanDefinition createBootstrapRebalanceListenerDef(BeanNames group) {
            return BeanDefinitionBuilder
                    .genericBeanDefinition(SnifferRebalanceListener.class)
                    .addConstructorArgValue(new ArrayList<>(group.names))
                    .addConstructorArgReference(group.snifferConsumer)
                    .getBeanDefinition();
        }

        /**
         * Creates a per-connection {@link KafkaSnifferConsumer} bean.
         *
         * <p>The first three constructor arguments ({@code connectionNames}, {@code consumer},
         * {@code rebalanceListener}) are per-connection and supplied explicitly via
         * RuntimeBeanReference, because they cannot be resolved by type: the context holds
         * N {@link KafkaConsumer} and N {@link SnifferRebalanceListener} beans, one per
         * bootstrap address.
         *
         * <p>The remaining three arguments ({@code topicRegistry}, {@code testMessagePoller},
         * {@code inboundMessageRegistry}) are singletons resolved by type via
         * {@link AbstractBeanDefinition#AUTOWIRE_CONSTRUCTOR}.
         *
         * <p><b>The order of the first three arguments is coupled to the field declaration
         * order in {@link KafkaSnifferConsumer}</b> (Lombok {@code @RequiredArgsConstructor}
         * generates the constructor in field order). When reordering per-connection fields,
         * update this factory too. See the marker comment in the class itself.
         */
        private AbstractBeanDefinition createBootstrapSnifferDef(BeanNames group) {
            AbstractBeanDefinition def = BeanDefinitionBuilder
                    .genericBeanDefinition(KafkaSnifferConsumer.class)
                    .addConstructorArgValue(new ArrayList<>(group.names))
                    .addConstructorArgReference(group.snifferConsumer)
                    .addConstructorArgReference(group.snifferRebalanceListener)
                    .getBeanDefinition();
            def.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            return def;
        }

        private AbstractBeanDefinition createAdminClientDef(BeanNames group) {
            AbstractBeanDefinition def = BeanDefinitionBuilder
                    .rootBeanDefinition(KafkaConfig.class)
                    .setFactoryMethod("createAdminClient")
                    .addConstructorArgReference(group.connectionProperties)
                    .getBeanDefinition();
            def.setDestroyMethodName("close");
            return def;
        }

        private AbstractBeanDefinition createAdminRepoDef(String adminClientRef) {
            return BeanDefinitionBuilder
                    .genericBeanDefinition(KafkaAdminRepository.class)
                    .addConstructorArgReference(adminClientRef)
                    .getBeanDefinition();
        }

        private AbstractBeanDefinition createStateCheckerDef(String rawBootstrap) {
            return BeanDefinitionBuilder
                    .genericBeanDefinition(InFlightLedgerScope.class)
                    .addConstructorArgReference("inFlightLedger")
                    .addConstructorArgValue(rawBootstrap)
                    .getBeanDefinition();
        }

        private AbstractBeanDefinition createWaitCompletionDef(String snifferName, String stateCheckerRef) {
            AbstractBeanDefinition def = BeanDefinitionBuilder
                    .genericBeanDefinition(KafkaWaitCompletion.class)
                    .addConstructorArgReference(snifferName)
                    .addConstructorArgReference(stateCheckerRef)
                    .getBeanDefinition();
            def.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            return def;
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }
    }

    private static class BeanNames {
        final String rawBootstrap;
        final String normalizedBootstrap;
        final List<String> names = new ArrayList<>();

        final String connectionProperties;
        final String adminClient;
        final String adminRepo;
        final String stateChecker;
        final String snifferConsumer;
        final String snifferRebalanceListener;
        final String sniffer;
        final String producer;
        final String waitCompletion;
        final String messageSender;

        BeanNames(String rawBootstrap, Map<String, Object> properties) {
            this.rawBootstrap = rawBootstrap;
            this.normalizedBootstrap = BootstrapUtil.normalize(rawBootstrap);
            String prefix = normalizedBootstrap + propertiesSuffix(properties);
            this.connectionProperties = prefix + "ConnectionProperties";
            this.adminClient = prefix + "AdminClient";
            this.adminRepo = prefix + "AdminRepo";
            this.stateChecker = prefix + "StateChecker";
            this.producer = prefix + "KafkaProducer";
            this.snifferConsumer = prefix + "SnifferConsumer";
            this.snifferRebalanceListener = prefix + "SnifferRebalanceListener";
            this.sniffer = prefix + "Sniffer";
            this.waitCompletion = prefix + "KafkaWaitCompletion";
            this.messageSender = prefix + "KafkaMessageSender";
        }

        /**
         * Returns a short deterministic suffix derived from the client
         * properties, or an empty string when properties are absent. Used only
         * to disambiguate bean names when multiple groups share the same
         * bootstrap address. The suffix is not used for equality; {@link PostProcessor.ClusterKey}
         * carries the full property map and compares by value, so a collision
         * here surfaces as a bean name clash (loud
         * {@code BeanDefinitionOverrideException}) rather than as silent
         * property mixing.
         */
        private static String propertiesSuffix(Map<String, Object> properties) {
            if (properties == null || properties.isEmpty()) {
                return "";
            }
            String canonical = properties.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(";"));
            return "_" + Integer.toHexString(canonical.hashCode());
        }
    }
}

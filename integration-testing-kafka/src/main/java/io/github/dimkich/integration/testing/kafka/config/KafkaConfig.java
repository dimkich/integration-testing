package io.github.dimkich.integration.testing.kafka.config;

import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.instrumentation.InstrumentationManager;
import io.github.dimkich.integration.testing.kafka.*;
import io.github.dimkich.integration.testing.kafka.inflight.agent.KafkaInFlightAgent;
import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedger;
import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedgerScope;
import io.github.dimkich.integration.testing.kafka.registry.KafkaObjectFactory;
import io.github.dimkich.integration.testing.kafka.registry.KafkaTopicRegistry;
import io.github.dimkich.integration.testing.kafka.serde.KafkaHeaderDeserializerConverterFactory;
import io.github.dimkich.integration.testing.kafka.serde.KafkaHeaderSerializerConverterFactory;
import io.github.dimkich.integration.testing.kafka.serde.KafkaRecordSerdeFactory;
import io.github.dimkich.integration.testing.kafka.serde.adapter.*;
import io.github.dimkich.integration.testing.kafka.serde.provider.*;
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
 * Spring configuration of the Kafka module: registers the serde factories, topic
 * registry, sniffer manager and serde adapters, and creates per-connection
 * infrastructure beans (producers, consumers, admin clients, sniffers, wait
 * completion and message senders) through {@link PostProcessor}.
 */
@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
@Import({
        KafkaRecordSerdeFactory.class,
        KafkaHeaderSerializerConverterFactory.class,
        KafkaHeaderDeserializerConverterFactory.class,
        KafkaObjectFactory.class,
        KafkaTopicRegistry.class,
        KafkaSnifferManager.class,

        SpringJsonKafkaValueSerializerProvider.class,
        SpringJsonKafkaValueDeserializerProvider.class,
        SpringXmlKafkaValueSerializerProvider.class,
        SpringXmlKafkaValueDeserializerProvider.class,
        SpringJsonKafkaHeaderSerializerProvider.class,
        SpringJsonKafkaHeaderDeserializerProvider.class,
        SpringXmlKafkaHeaderSerializerProvider.class,
        SpringXmlKafkaHeaderDeserializerProvider.class,

        KafkaSerializerToRecordPartSerializerAdapter.class,
        KafkaDeserializerToRecordPartDeserializerAdapter.class,
        KafkaSerializerToKafkaHeaderSerializerAdapter.class,
        KafkaDeserializerToKafkaHeaderDeserializerAdapter.class,
        KafkaHeaderMapperToKafkaHeaderSerializerAdapter.class,
        KafkaHeaderMapperToKafkaHeaderDeserializerAdapter.class,
        TestSerdeConverterToKafkaHeaderSerializerAdapter.class,
        TestSerdeConverterToKafkaHeaderDeserializerAdapter.class,

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

        // at least one plugin is always applicable, so an empty list means install() has not run yet
        private void assertInstrumentationInstalled() {
            if (InstrumentationManager.getActivePlugins().isEmpty()) {
                throw new IllegalStateException(
                        "Kafka context is being built but no instrumentation plugins are active. "
                                + "This usually means JunitExtension.beforeAll has not run yet "
                                + "(check @Order on JunitExtension), or the test was started "
                                + "without @IntegrationTesting.");
            }
        }

        // ConnectionBootstrap, not ConnectionProperties: the manual binder does not see
        // @ConfigurationPropertiesBinding beans, so binding java.lang.reflect.Type fields would fail
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

        // normalized address + client properties: connections with different security/acks/ids must not share clients
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

        // the first three args are per-connection (N beans of each type) and their order is coupled to the
        // field order of KafkaSnifferConsumer; the rest are autowired singletons
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

        // disambiguates bean names for groups on the same bootstrap address; equality lives in
        // ClusterKey, so a suffix collision surfaces as a bean name clash instead of silent mixing
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

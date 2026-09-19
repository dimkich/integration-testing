package io.github.dimkich.integration.testing.kafka.serde.misc;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.transaction.KafkaTransactionManager;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Import(MiscProcessor.class)
public class MiscSerDeConfig {

    @Value("${embedded.kafka.brokerList}")
    private String brokerList;

    @Bean
    public KafkaAdmin.NewTopics miscTopics() {
        return new KafkaAdmin.NewTopics(
                topic("misc-pattern-service-orders"),
                topic("misc-pattern-processed"),
                topic("misc-tx-out"),
                topic("misc-tx-offsets-in"),
                topic("misc-tx-offsets-out"),
                topic("misc-assign"),
                topic("misc-assign-result")
        );
    }

    private static NewTopic topic(String name) {
        return new NewTopic(name, 1, (short) 1);
    }

    @Bean
    public ProducerFactory<String, String> miscTxProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx-misc-");

        DefaultKafkaProducerFactory<String, String> factory = new DefaultKafkaProducerFactory<>(props);
        factory.setTransactionIdPrefix("tx-misc-");
        return factory;
    }

    @Bean
    public KafkaTemplate<String, String> miscTxKafkaTemplate() {
        return new KafkaTemplate<>(miscTxProducerFactory());
    }

    @Bean
    public ProducerFactory<String, String> txOffsetsProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx-misc-offsets-");

        DefaultKafkaProducerFactory<String, String> factory = new DefaultKafkaProducerFactory<>(props);
        factory.setTransactionIdPrefix("tx-misc-offsets-");
        return factory;
    }

    @Bean
    public KafkaTransactionManager<String, String> txOffsetsKafkaTransactionManager(
            ProducerFactory<String, String> txOffsetsProducerFactory) {
        return new KafkaTransactionManager<>(txOffsetsProducerFactory);
    }

    @Bean
    public KafkaTemplate<String, String> txOffsetsKafkaTemplate(
            ProducerFactory<String, String> txOffsetsProducerFactory) {
        return new KafkaTemplate<>(txOffsetsProducerFactory);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> txOffsetsListenerContainerFactory(
            ConsumerFactory<String, String> textConsumerFactory,
            KafkaTransactionManager<String, String> txOffsetsKafkaTransactionManager) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(textConsumerFactory);
        factory.getContainerProperties().setTransactionManager(txOffsetsKafkaTransactionManager);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }
}

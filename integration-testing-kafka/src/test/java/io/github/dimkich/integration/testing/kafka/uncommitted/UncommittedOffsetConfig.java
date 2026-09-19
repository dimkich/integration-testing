package io.github.dimkich.integration.testing.kafka.uncommitted;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class UncommittedOffsetConfig {

    @Value("${embedded.kafka.brokerList}")
    private String brokerList;

    @Bean
    public UncommittedOffsetProcessor uncommittedOffsetProcessor(KafkaTemplate<String, String> uncommittedKafkaTemplate) {
        return new UncommittedOffsetProcessor(uncommittedKafkaTemplate);
    }

    @Bean
    public DefaultErrorHandler uncommittedErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler((record, exception) -> {
        }, new FixedBackOff(0L, 0L));
        handler.setAckAfterHandle(false);
        return handler;
    }

    @Bean
    public DefaultKafkaConsumerFactory<String, String> uncommittedConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> uncommittedListenerContainerFactory(
            DefaultKafkaConsumerFactory<String, String> uncommittedConsumerFactory,
            DefaultErrorHandler uncommittedErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(uncommittedConsumerFactory);
        factory.setCommonErrorHandler(uncommittedErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }

    @Bean
    public DefaultKafkaProducerFactory<String, String> uncommittedProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> uncommittedKafkaTemplate(
            DefaultKafkaProducerFactory<String, String> uncommittedProducerFactory) {
        return new KafkaTemplate<>(uncommittedProducerFactory);
    }

    @Bean
    public KafkaAdmin.NewTopics uncommittedTopics() {
        return new KafkaAdmin.NewTopics(topic("misc-uncommitted-in"), topic("misc-uncommitted-out"));
    }

    private static NewTopic topic(String name) {
        return new NewTopic(name, 1, (short) 1);
    }
}

package io.github.dimkich.integration.testing.kafka.serde.envelope;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.converter.RecordMessageConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * Application-side configuration for the binary envelope scenario: consumes
 * {@code bin-envelope-in} with an envelope-aware deserializer and produces enveloped
 * bytes to {@code bin-envelope-out}.
 */
@Configuration
@Import(EnvelopeProcessor.class)
public class EnvelopeSerDeConfig {

    @Value("${embedded.kafka.brokerList}")
    private String brokerList;

    @Bean
    public ConsumerFactory<String, String> envelopeConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EnvelopeStringDeserializer.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> envelopeListenerContainerFactory(
            ConsumerFactory<String, String> envelopeConsumerFactory,
            RecordMessageConverter textRecordMessageConverter) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(envelopeConsumerFactory);
        factory.setRecordMessageConverter(textRecordMessageConverter);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }

    @Bean
    public ProducerFactory<String, String> envelopeProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, EnvelopeStringSerializer.class.getName());
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> envelopeKafkaTemplate(ProducerFactory<String, String> envelopeProducerFactory) {
        return new KafkaTemplate<>(envelopeProducerFactory);
    }

    @Bean
    public KafkaAdmin.NewTopics envelopeTopics() {
        return new KafkaAdmin.NewTopics(
                topic("bin-envelope-in"), topic("bin-envelope-out"), topic("bin-envelope-macro-in")
        );
    }

    private static NewTopic topic(String name) {
        return new NewTopic(name, 1, (short) 1);
    }
}

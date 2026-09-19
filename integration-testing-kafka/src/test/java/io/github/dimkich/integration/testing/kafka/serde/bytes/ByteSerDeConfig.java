package io.github.dimkich.integration.testing.kafka.serde.bytes;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Import(ByteProcessor.class)
public class ByteSerDeConfig {

    @Value("${embedded.kafka.brokerList}")
    private String brokerList;

    @Bean
    public ConsumerFactory<String, byte[]> byteConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ByteArrayDeserializer()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> byteListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, byte[]>();
        factory.setConsumerFactory(byteConsumerFactory());
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }

    @Bean
    public ProducerFactory<String, byte[]> byteProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new ByteArraySerializer()
        );
    }

    @Bean
    public KafkaTemplate<String, byte[]> byteKafkaTemplate() {
        return new KafkaTemplate<>(byteProducerFactory());
    }

    @Bean
    public KafkaAdmin.NewTopics byteTopics() {
        return new KafkaAdmin.NewTopics(
                topic("byte-record-in"), topic("byte-record-out"),
                topic("byte-parts-in"), topic("byte-parts-out"),
                topic("byte-native-in"), topic("byte-native-out")
        );
    }

    private static NewTopic topic(String name) {
        return new NewTopic(name, 1, (short) 1);
    }
}

package io.github.dimkich.integration.testing.kafka.serde.text;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Import(TextProcessor.class)
public class TextSerDeConfig {

    @Value("${embedded.kafka.brokerList}")
    private String brokerList;

    @Bean
    public ConsumerFactory<String, String> textConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> textListenerContainerFactory(
            ConsumerFactory<String, String> textConsumerFactory,
            RecordMessageConverter textRecordMessageConverter) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(textConsumerFactory);
        factory.setRecordMessageConverter(textRecordMessageConverter);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }

    @Bean
    public RecordMessageConverter textRecordMessageConverter() {
        return new RecordMessageConverter() {
            @Override
            @SuppressWarnings("NullableProblems")
            public Message<?> toMessage(ConsumerRecord<?, ?> record, Acknowledgment acknowledgment,
                                        org.apache.kafka.clients.consumer.Consumer<?, ?> consumer, Type payloadType) {
                Object value = record.value();
                Object key = record.key();
                Map<String, Object> msgHeaders = new HashMap<>();
                msgHeaders.put(KafkaHeaders.RECEIVED_KEY, key);
                msgHeaders.put(KafkaHeaders.RECEIVED_TOPIC, record.topic());
                Headers headers = record.headers();
                if (headers != null) {
                    headers.forEach(h -> {
                        String headerKey = h.key();
                        String headerValue = h.value() == null ? null : new String(h.value(), StandardCharsets.UTF_8);
                        if (msgHeaders.containsKey(headerKey)) {
                            Object existing = msgHeaders.get(headerKey);
                            List<Object> list = new ArrayList<>();
                            if (existing instanceof List<?> existingList) {
                                list.addAll(existingList);
                            } else {
                                list.add(existing);
                            }
                            list.add(headerValue);
                            msgHeaders.put(headerKey, list);
                        } else {
                            msgHeaders.put(headerKey, headerValue);
                        }
                    });
                }
                if (value == null) {
                    return new Message<>() {
                        @Override
                        @SuppressWarnings("NullableProblems")
                        public Object getPayload() {
                            return null;
                        }

                        @Override
                        @SuppressWarnings("NullableProblems")
                        public MessageHeaders getHeaders() {
                            return new MessageHeaders(msgHeaders);
                        }
                    };
                }
                return new GenericMessage<>(value, msgHeaders);
            }

            @Override
            public ProducerRecord<?, ?> fromMessage(Message<?> message, String defaultTopic) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Bean
    public ProducerFactory<String, String> textProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> textKafkaTemplate(ProducerFactory<String, String> textProducerFactory) {
        return new KafkaTemplate<>(textProducerFactory);
    }

    @Bean
    public StringSerializer textStringSerializer() {
        return new StringSerializer();
    }

    @Bean
    public StringDeserializer textStringDeserializer() {
        return new StringDeserializer();
    }


    @Bean
    public KafkaAdmin.NewTopics topics() {
        return new KafkaAdmin.NewTopics(
                topic("text-record-in"), topic("text-record-out"),
                topic("text-parts-in"), topic("text-parts-out"),
                topic("text-native-in"), topic("text-native-out"),
                topic("text-string-record-in"), topic("text-string-record-out"),
                topic("text-default-in"), topic("text-default-out")
        );
    }

    private static NewTopic topic(String name) {
        return new NewTopic(name, 1, (short) 1);
    }
}

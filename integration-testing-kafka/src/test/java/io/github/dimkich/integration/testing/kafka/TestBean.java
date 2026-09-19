package io.github.dimkich.integration.testing.kafka;

import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.KafkaNull;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestBean {

    @Autowired
    private ApplicationContext context;

    public int getZero() {
        return 0;
    }

    public void sendOutbound(String templateBeanName, String topic, Object key, Object payload) {
        sendOutbound(templateBeanName, topic, key, payload, null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void sendOutbound(String templateBeanName, String topic, Object key, Object payload, Map<String, String> headers) {
        KafkaTemplate template = context.getBean(templateBeanName, KafkaTemplate.class);
        List<Header> kafkaHeaders = new ArrayList<>();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                kafkaHeaders.add(new RecordHeader(entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8)));
            }
        }
        ProducerRecord<Object, Object> record = new ProducerRecord<>(topic, null, null, key, payload, kafkaHeaders);
        template.send(record);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void sendOutboundNullHeader(String templateBeanName, String topic, Object key, Object payload, String headerName) {
        KafkaTemplate template = context.getBean(templateBeanName, KafkaTemplate.class);
        List<Header> kafkaHeaders = new ArrayList<>();
        kafkaHeaders.add(new RecordHeader(headerName, null));
        ProducerRecord<Object, Object> record = new ProducerRecord<>(topic, null, null, key, payload, kafkaHeaders);
        template.send(record);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void sendOutboundSpring(String templateBeanName, String topic, Object key, Object payload, Map<String, String> headers) {
        KafkaTemplate template = context.getBean(templateBeanName, KafkaTemplate.class);
        Object messagePayload = payload != null ? payload : KafkaNull.INSTANCE;
        MessageBuilder builder = MessageBuilder.withPayload(messagePayload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, key);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.setHeader(entry.getKey(), entry.getValue());
            }
        }
        template.send(builder.build());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void sendOutboundSpringMultiHeader(String templateBeanName, String topic, Object key, Object payload,
                                              String headerName, String value1, String value2) {
        KafkaTemplate template = context.getBean(templateBeanName, KafkaTemplate.class);
        Object messagePayload = payload != null ? payload : KafkaNull.INSTANCE;
        MessageBuilder builder = MessageBuilder.withPayload(messagePayload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, key)
                .setHeader(headerName, List.of(value1, value2));
        template.send(builder.build());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void sendOffsetsInTransaction(String templateBeanName, String topic, Object key, Object payload, String groupId) {
        KafkaTemplate template = context.getBean(templateBeanName, KafkaTemplate.class);
        ProducerFactory producerFactory = template.getProducerFactory();
        try (Producer producer = producerFactory.createProducer()) {
            producer.beginTransaction();
            producer.send(new ProducerRecord<>(topic, key, payload));
            producer.sendOffsetsToTransaction(
                    Map.of(new TopicPartition(topic, 0), new OffsetAndMetadata(0)), new ConsumerGroupMetadata(groupId));
            producer.commitTransaction();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void sendInTransaction(String templateBeanName, String topic, Object key, Object payload, boolean rollback) {
        KafkaTemplate template = context.getBean(templateBeanName, KafkaTemplate.class);
        template.executeInTransaction(t -> {
            if (!rollback) {
                t.send(topic, key, payload);
            }
            if (rollback) {
                throw new RuntimeException("Rollback requested");
            }
            return true;
        });
    }
}

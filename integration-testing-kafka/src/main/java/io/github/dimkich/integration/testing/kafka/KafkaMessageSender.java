package io.github.dimkich.integration.testing.kafka;

import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.kafka.registry.KafkaTopicMetadata;
import io.github.dimkich.integration.testing.kafka.registry.KafkaTopicRegistry;
import io.github.dimkich.integration.testing.message.AbstractMessage;
import io.github.dimkich.integration.testing.message.TestMessageSender;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Set;

/**
 * Test message sender that publishes {@link KafkaRecord} messages to Kafka using
 * the record serializer configured for the target topic.
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("rawtypes")
public class KafkaMessageSender implements TestMessageSender {
    private final Set<String> connectionNames;
    private final KafkaProducer<byte[], byte[]> producer;
    private final KafkaTopicRegistry kafkaTopicRegistry;
    private final InboundMessageRegistry inboundMessageRegistry;
    private final DateTimeService dateTimeService;

    @Override
    public boolean canSend(AbstractMessage message) {
        if (!(message instanceof KafkaRecord kafkaRecord)) {
            return false;
        }
        return connectionNames.contains(kafkaRecord.getConnection());
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public void sendInboundMessage(AbstractMessage message) {
        if (!(message instanceof KafkaRecord kafkaRecord)) {
            throw new IllegalArgumentException("Expected KafkaRecord, got " + message.getClass());
        }

        String topic = kafkaRecord.getTopic();
        String connectionName = kafkaRecord.getConnection();

        KafkaTopicMetadata metadata = kafkaTopicRegistry.getMetadata(connectionName, topic);
        TestSerdeConverter<KafkaRecord, ProducerRecord, TestSerdeContext> serializer = metadata.getSerializer();

        log.debug("Sending raw serialized message to topic [{}] via connection [{}]", topic, connectionName);

        ProducerRecord<byte[], byte[]> record = serializer.convert(kafkaRecord, TestSerdeContext.EMPTY);

        if (record.timestamp() == null && dateTimeService.getDateTime() != null) {
            long fakeTimestamp = dateTimeService.getDateTime().toInstant().toEpochMilli();
            record = new ProducerRecord<>(record.topic(), record.partition(), fakeTimestamp,
                    record.key(), record.value(), record.headers());
        }

        ProducerRecord<byte[], byte[]> finalRecord = record;
        inboundMessageRegistry.sendAndRegister(() -> producer.send(finalRecord).get());
    }
}

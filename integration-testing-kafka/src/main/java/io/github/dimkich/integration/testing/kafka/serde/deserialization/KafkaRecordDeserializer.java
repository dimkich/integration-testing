package io.github.dimkich.integration.testing.kafka.serde.deserialization;

import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Deserializes a raw Kafka {@link ConsumerRecord} into a {@link KafkaRecord} with key,
 * value and headers.
 */
public interface KafkaRecordDeserializer {
    /**
     * Deserializes the given record.
     *
     * @param record the raw record read by the sniffer
     * @return the deserialized message
     */
    KafkaRecord deserialize(ConsumerRecord<byte[], byte[]> record);
}

package io.github.dimkich.integration.testing.kafka.serde.serialization;

import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * Serializes a {@link KafkaRecord} (topic, key, value, headers) into a raw Kafka
 * {@link ProducerRecord} with byte-array payloads.
 */
public interface KafkaRecordSerializer {
    /**
     * Serializes the given message.
     *
     * @param message the message to serialize
     * @return the producer record with serialized key and value
     */
    ProducerRecord<byte[], byte[]> serialize(KafkaRecord message);
}

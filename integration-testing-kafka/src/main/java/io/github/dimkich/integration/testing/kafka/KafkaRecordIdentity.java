package io.github.dimkich.integration.testing.kafka;

/**
 * Identity assigned to a Kafka record by the broker; used by
 * {@link io.github.dimkich.integration.testing.message.TestMessages} to filter records
 * sent by the test itself out of the captured outbound records.
 */
public record KafkaRecordIdentity(String topic, Integer partition, Long offset) {
}

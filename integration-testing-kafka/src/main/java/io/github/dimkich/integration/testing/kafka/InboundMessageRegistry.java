package io.github.dimkich.integration.testing.kafka;

import lombok.SneakyThrows;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Tracks offsets of messages sent by the test itself, so the sniffer can tell them
 * apart from messages produced by the system under test.
 */
public class InboundMessageRegistry {

    /**
     * Identifies a single Kafka record by topic, partition and offset.
     */
    public record TopicPartitionOffset(String topic, int partition, long offset) {
    }

    private final Set<TopicPartitionOffset> registeredOffsets = new HashSet<>();

    /**
     * Returns whether the record at the given position was sent by the test.
     *
     * @param topic the record topic
     * @param partition the record partition
     * @param offset the record offset
     * @return {@code true} if the offset was registered by
     *         {@link #sendAndRegister(Callable)}
     */
    public synchronized boolean contains(String topic, int partition, long offset) {
        return registeredOffsets.contains(new TopicPartitionOffset(topic, partition, offset));
    }

    /**
     * Runs {@code send} and records the resulting offset under the same monitor.
     * Because sending happens inside the synchronized block, the sniffer cannot
     * observe a record on the broker before its offset is registered: either
     * sending has not completed yet (sniffer blocks on the monitor), or it has,
     * and the offset is already in the set.
     */
    @SneakyThrows
    public synchronized void sendAndRegister(Callable<RecordMetadata> send) {
        RecordMetadata m = send.call();
        registeredOffsets.add(new TopicPartitionOffset(m.topic(), m.partition(), m.offset()));
    }

    /**
     * Clears all registered offsets before a new wait cycle.
     */
    public synchronized void reset() {
        registeredOffsets.clear();
    }
}

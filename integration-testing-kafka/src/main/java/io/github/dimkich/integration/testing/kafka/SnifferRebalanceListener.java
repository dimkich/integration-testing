package io.github.dimkich.integration.testing.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Rebalance listener of the sniffer: on assignment it positions partitions so that
 * pre-existing history is skipped at startup, while topics created mid-session are
 * read from the beginning. Remembers read offsets across rebalances to resume
 * consumption where it stopped.
 */
@Slf4j
@RequiredArgsConstructor
public class SnifferRebalanceListener implements ConsumerRebalanceListener {
    private final List<String> connectionNames;
    private final KafkaConsumer<byte[], byte[]> consumer;
    private final Set<TopicPartition> initializedPartitions = ConcurrentHashMap.newKeySet();
    private final Map<TopicPartition, Long> lastReadOffsets = new ConcurrentHashMap<>();

    /**
     * Set to true after the very first rebalance callback, whether it carried
     * partitions or not. Distinguishes the subscribe-driven initial assignment
     * (topics that existed before the sniffer started, may contain history)
     * from subsequent rebalances (topics that appeared mid-session, all their
     * content belongs to the current test run).
     */
    private final AtomicBoolean initialAssignmentSeen = new AtomicBoolean(false);

    /**
     * Forgets all tracked partitions and offsets, so the next rebalance is treated as
     * the initial one.
     */
    public void clear() {
        initializedPartitions.clear();
        lastReadOffsets.clear();
        initialAssignmentSeen.set(false);
    }

    /**
     * Remembers the read position of a partition, used to resume it after a rebalance.
     *
     * @param tp the partition
     * @param offset the next offset to read
     */
    public void updateOffset(TopicPartition tp, long offset) {
        lastReadOffsets.put(tp, offset);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        log.trace("Sniffer [{}]: Partitions revoked: {}", connectionNames, partitions);
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        log.trace("Sniffer [{}]: Partitions assigned: {}", connectionNames, partitions);

        boolean isFirstEver = initialAssignmentSeen.compareAndSet(false, true);

        for (TopicPartition tp : partitions) {
            if (initializedPartitions.contains(tp)) {
                Long lastOffset = lastReadOffsets.get(tp);
                if (lastOffset != null) {
                    consumer.seek(tp, lastOffset);
                }
                continue;
            }

            initializedPartitions.add(tp);

            if (isFirstEver) {
                consumer.seekToEnd(List.of(tp));
                long endOffset = consumer.position(tp);
                lastReadOffsets.put(tp, endOffset);
                log.debug("Sniffer [{}]: Partition [{}] initialized at end offset {} "
                                + "(skip pre-existing history)",
                        connectionNames, tp, endOffset);
            } else {
                consumer.seek(tp, 0L);
                lastReadOffsets.put(tp, 0L);
                log.debug("Sniffer [{}]: Partition [{}] initialized at offset 0 "
                                + "(topic created mid-session)",
                        connectionNames, tp);
            }
        }
    }
}
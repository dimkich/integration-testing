package io.github.dimkich.integration.testing.kafka.inflight.ledger;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tracks broker-side offsets observed by the in-flight ledger:
 * <ul>
 *   <li><b>end offsets</b> — the next-to-consume offset after the latest message
 *       acknowledged by any producer (recorded as {@code offset + 1});</li>
 *   <li><b>committed offsets</b> — per consumer group, the offset committed by the
 *       application's consumers;</li>
 *   <li><b>seen partitions</b> — the union of every partition mentioned by either
 *       path, used to signal the sniffer which partitions need refreshing.</li>
 * </ul>
 *
 * <p>End offsets are recorded from both the non-transactional acknowledgement and
 * the transactional commit paths, and merged with {@code Math::max} — the ledger
 * never regresses.
 *
 * <p>Package-private: detail of {@link ClusterState}, not part of the ledger's
 * public surface.
 */
@Slf4j
final class OffsetLedger {

    private final ConcurrentHashMap<TopicPartition, Long> endOffsets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<TopicPartition, Long>> committedByGroup =
            new ConcurrentHashMap<>();
    private final Set<TopicPartition> seenPartitions = ConcurrentHashMap.newKeySet();

    /**
     * Records the broker end offset for a partition as {@code offset + 1}.
     *
     * <p>Deliberately does <em>not</em> touch {@link PendingSends}: end offsets are
     * recorded from both the non-transactional acknowledgement and the transactional
     * commit paths, but only the former is paired with an {@code onSend}. In-flight
     * resolution is done by {@code PendingSends.decrement}.
     *
     * @param tp     the observed topic partition
     * @param offset the acknowledged offset (exclusive end becomes {@code offset + 1})
     */
    void recordEndOffset(TopicPartition tp, long offset) {
        endOffsets.merge(tp, offset + 1, Math::max);
        seenPartitions.add(tp);
        log.debug("end offset [{}] = {}", tp, offset + 1);
    }

    void recordCommit(String groupId, TopicPartition tp, long offset) {
        committedByGroup
                .computeIfAbsent(groupId, k -> new ConcurrentHashMap<>())
                .put(tp, offset);
        seenPartitions.add(tp);
    }

    /** Registers a partition observed on a producer send before partitioning. */
    void recordSeenPartition(TopicPartition tp) {
        seenPartitions.add(tp);
    }

    /**
     * Live view of {@code (partition, endOffset)} entries.
     *
     * <p>Used by {@link ClusterState#hasLag()} to iterate partitions without
     * copying the map; the underlying {@link ConcurrentHashMap} iterator is weakly
     * consistent, so a concurrent {@code recordEndOffset} may or may not be visible
     * in the current pass — same semantics as before extraction.
     */
    Set<Map.Entry<TopicPartition, Long>> endOffsetEntries() {
        return Collections.unmodifiableSet(endOffsets.entrySet());
    }

    long committedOffset(String groupId, TopicPartition tp) {
        Map<TopicPartition, Long> m = committedByGroup.get(groupId);
        return m == null ? 0L : m.getOrDefault(tp, 0L);
    }

    /** All partitions ever mentioned by an end-offset or commit event. */
    Set<TopicPartition> seenPartitions() {
        return Collections.unmodifiableSet(seenPartitions);
    }

    /** Drops committed offsets of a dead group; end offsets stay (broker state). */
    void removeGroup(String groupId) {
        committedByGroup.remove(groupId);
    }

    /**
     * Renders {@code "endOffsets: [...], committed: [...]"} — the tail of
     * {@link ClusterState#describe()}.
     */
    String describe() {
        return "endOffsets: [" + endOffsets.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .sorted(Comparator.comparing(e -> e.split("=")[0]))
                .collect(Collectors.joining(", "))
                + "], committed: [" + committedByGroup.entrySet().stream()
                .flatMap(e -> e.getValue().entrySet().stream()
                        .map(c -> e.getKey() + "@" + c.getKey() + "=" + c.getValue()))
                .sorted()
                .collect(Collectors.joining(", "))
                + "]";
    }
}
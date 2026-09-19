package io.github.dimkich.integration.testing.kafka.inflight.ledger;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Accumulates transactional offsets between the moment they are accepted by the
 * client and the moment the transaction is committed or aborted:
 * <ul>
 *   <li><b>end offsets</b> of acknowledged producer sends, keyed by
 *       {@code transactional.id};</li>
 *   <li><b>consumer-group offsets</b> submitted with
 *       {@code sendOffsetsToTransaction}, keyed by {@code transactional.id}
 *       then {@code group.id}.</li>
 * </ul>
 *
 * <p>Transactional producers do not publish offsets until
 * {@code commitTransaction}, so both kinds are buffered here. On commit the
 * caller takes the buffers via {@link #take(String)} and
 * {@link #takeGroupOffsets(String)} and flushes them into the ledger; on
 * abort/failed commit they are discarded via {@link #discard(String)}.
 *
 * <p><b>Scope:</b> an instance belongs to exactly one {@link ClusterState}, so
 * no bootstrap component is needed in the key. A cluster-scoped reset (via
 * {@link #clear()}) affects only this cluster's pending offsets, leaving sibling
 * clusters untouched.
 *
 * <p><b>Leak prevention.</b> The buffer is keyed by {@code transactional.id},
 * which is otherwise linked to a producer only via a weak registry entry. A
 * producer that is closed or GC'd without commit/abort cannot be cleaned via
 * that key later. Two paths bound the leak:
 * <ul>
 *   <li>{@link #discard(String)} is invoked on producer close
 *       (see the producer interceptor's {@code close()} hook);</li>
 *   <li>{@link #clear()} drops everything at each test boundary,
 *       reclaiming entries orphaned by GC'd producers.</li>
 * </ul>
 * The buffered offsets are transient by nature: at a test boundary there should
 * be no open transaction, so a cluster-scoped clear is safe.
 *
 * <p>Package-private: detail of {@link ClusterState}, not part of the ledger's
 * public surface.
 */
final class TransactionTracker {

    private final ConcurrentHashMap<String, ConcurrentHashMap<TopicPartition, Long>> pending =
            new ConcurrentHashMap<>();

    /**
     * Buffered consumer-group offsets of transactional offset commits, keyed by
     * {@code transactional.id} then {@code group.id}. Values are the
     * next-to-consume positions exactly as passed to
     * {@code sendOffsetsToTransaction} (already exclusive), so on commit they
     * are flushed without the {@code +1} conversion applied to message offsets.
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<TopicPartition, Long>>>
            pendingGroupOffsets = new ConcurrentHashMap<>();

    /**
     * Buffers the acknowledged end offset of one partition for a transactional
     * send.
     *
     * @param transactionalId the producer's {@code transactional.id}; ignored when null
     * @param tp the acknowledged topic partition
     * @param offset the acknowledged end offset
     */
    void record(String transactionalId, TopicPartition tp, long offset) {
        if (transactionalId == null) {
            return;
        }
        pending.computeIfAbsent(transactionalId, k -> new ConcurrentHashMap<>())
                .merge(tp, offset, Math::max);
    }

    /**
     * Removes and returns the buffered offsets of one producer. The caller
     * decides whether to flush them (successful commit) or discard them
     * (failed commit, abort).
     *
     * <p>Always removes the entry, regardless of whether offsets were present;
     * an unknown or already-committed {@code transactionalId} yields an empty
     * map.
     *
     * @param transactionalId the producer's {@code transactional.id}; ignored when null
     * @return immutable snapshot of the buffered offsets, never {@code null}
     */
    Map<TopicPartition, Long> take(String transactionalId) {
        if (transactionalId == null) {
            return Collections.emptyMap();
        }
        ConcurrentHashMap<TopicPartition, Long> removed = pending.remove(transactionalId);
        return removed == null ? Collections.emptyMap() : removed;
    }

    /**
     * Buffers the consumer-group offsets of one transactional offset commit.
     *
     * <p>The last value per partition wins: {@code sendOffsetsToTransaction}
     * called several times within one transaction carries the offsets to commit,
     * so a later call supersedes an earlier one for the same partition.
     *
     * @param transactionalId the producer's {@code transactional.id}; ignored when null
     * @param groupId the consumer group whose offsets are being committed; ignored when null
     * @param offsets next-to-consume offsets supplied to {@code sendOffsetsToTransaction}
     */
    void recordGroupOffsets(String transactionalId, String groupId,
                            Map<TopicPartition, OffsetAndMetadata> offsets) {
        if (transactionalId == null || groupId == null || offsets == null || offsets.isEmpty()) {
            return;
        }
        ConcurrentHashMap<TopicPartition, Long> byPartition = pendingGroupOffsets
                .computeIfAbsent(transactionalId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(groupId, k -> new ConcurrentHashMap<>());
        offsets.forEach((tp, offsetAndMetadata) -> byPartition.put(tp, offsetAndMetadata.offset()));
    }

    /**
     * Removes and returns the buffered group offsets of one producer, keyed by
     * group id. The caller flushes them on successful commit; a missing entry
     * yields an empty map.
     *
     * @param transactionalId the producer's {@code transactional.id}; ignored when null
     * @return snapshot of the buffered offsets, never {@code null}
     */
    Map<String, Map<TopicPartition, Long>> takeGroupOffsets(String transactionalId) {
        if (transactionalId == null) {
            return Collections.emptyMap();
        }
        ConcurrentHashMap<String, ConcurrentHashMap<TopicPartition, Long>> removed =
                pendingGroupOffsets.remove(transactionalId);
        if (removed == null) {
            return Collections.emptyMap();
        }
        return new HashMap<>(removed);
    }

    /**
     * Discards the buffered end offsets and group offsets of one producer. Used
     * on abort and on producer close without commit/abort.
     *
     * @param transactionalId the producer's {@code transactional.id}; ignored when null
     */
    void discard(String transactionalId) {
        if (transactionalId != null) {
            pending.remove(transactionalId);
            pendingGroupOffsets.remove(transactionalId);
        }
    }

    /**
     * Drops all buffered offsets for this cluster. Called at test boundaries to
     * reclaim entries orphaned by producers closed or GC'd without commit/abort.
     */
    void clear() {
        pending.clear();
        pendingGroupOffsets.clear();
    }
}

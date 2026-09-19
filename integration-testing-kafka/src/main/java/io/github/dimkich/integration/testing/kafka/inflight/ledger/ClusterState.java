package io.github.dimkich.integration.testing.kafka.inflight.ledger;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Holds the observed state of a single Kafka cluster (identified by a normalized
 * bootstrap server address). Tracks in-flight send counts, end offsets, committed
 * offsets per consumer group, topic subscriptions, and active consumer counts.
 *
 * <p><b>What {@link #clear()} resets:</b> The pending send counts, the
 * subscription pattern cache, and the pending transactional offsets &mdash; the
 * transient per-test fields. End offsets, committed offsets, subscriptions, and
 * active counts are deliberately preserved because:
 * <ul>
 *   <li>SUT consumers are created once at context startup, not per test.</li>
 *   <li>Committed/end offsets reflect monotonic broker-side state that must
 *       persist across test iterations in UntilStopped/loop mode.</li>
 * </ul>
 *
 * <p><b>Dead group cleanup:</b> {@link #decrementActiveConsumer(String)} removes
 * the group from all maps when its active count drops to zero, preventing stale
 * groups from polluting {@link #hasLag()} and {@link #getAllGroupIds()}.
 *
 * @see InFlightLedger
 */
public class ClusterState {

    /**
     * Topic subscriptions of each consumer group. Delegated to
     * {@link TopicSubscriptions} which owns exact/pattern lookup and the
     * per-group pattern cache.
     */
    private final TopicSubscriptions subscriptions = new TopicSubscriptions();

    /**
     * End offsets, committed offsets and seen partitions. Delegated to
     * {@link OffsetLedger} which owns the merge-by-max invariant and the
     * per-group committed map.
     */
    private final OffsetLedger offsets = new OffsetLedger();

    /**
     * In-flight producer sends. Delegated to {@link PendingSends} which owns the
     * per-topic counting invariant; see its Javadoc for the rationale.
     */
    private final PendingSends pendingSends = new PendingSends();

    /**
     * Per-group active consumer count. Delegated to {@link ConsumerLiveness}
     * which owns the increment/decrement-to-zero invariant.
     */
    private final ConsumerLiveness liveness = new ConsumerLiveness();

    /**
     * Pending transactional offsets observed on this cluster. Owned here (not
     * globally) so that a cluster-scoped reset or release affects only this
     * cluster's pending state.
     */
    private final TransactionTracker transactionTracker = new TransactionTracker();

    /**
     * Assignment snapshots of the consumers observed on this cluster, used by the
     * wait completion to answer group readiness without contacting the broker.
     */
    private final ConsumerAssignments assignments = new ConsumerAssignments();

    /**
     * Stateless lag computation over {@link #pendingSends}, {@link #offsets},
     * {@link #subscriptions} and {@link #liveness}. Constructed by field
     * initializer order; declaration order above matters.
     */
    private final LagCalculator lag = new LagCalculator(pendingSends, offsets, subscriptions, liveness);

    // ----------------------------------------------------------------
    // Data path
    // ----------------------------------------------------------------

    /**
     * Registers a producer send, incrementing the topic's in-flight count.
     *
     * @param topic the target topic
     * @param partition the explicit partition or {@code null} when the producer chooses
     */
    public void onSend(String topic, Integer partition) {
        pendingSends.increment(topic);
        if (partition != null) {
            offsets.recordSeenPartition(new TopicPartition(topic, partition));
        }
    }

    /**
     * Records a consumer offset commit for a group.
     *
     * @param groupId the consumer group
     * @param tp the committed partition
     * @param offset the committed offset
     */
    public void onCommit(String groupId, TopicPartition tp, long offset) {
        offsets.recordCommit(groupId, tp, offset);
    }

    /**
     * Records the broker end offset for a partition as {@code offset + 1}.
     *
     * <p>Deliberately does <em>not</em> touch the pending send counts: end offsets are
     * recorded from both the non-transactional acknowledgement and the transactional
     * commit paths, but only the former is paired with an {@link #onSend}.
     * Decrementing in-flight counts here would let a transactional commit clear a
     * still-in-flight non-transactional send. In-flight resolution is done by
     * {@link #completeSend}.
     *
     * @param tp the observed topic partition
     * @param offset the acknowledged offset (exclusive end becomes {@code offset + 1})
     */
    public void recordEndOffset(TopicPartition tp, long offset) {
        offsets.recordEndOffset(tp, offset);
    }

    /**
     * Resolves one in-flight send for a topic by decrementing its pending count.
     * The topic is removed once its count reaches zero.
     *
     * <p>Symmetrical to {@link #onSend}. Safe to call out-of-order or for an unknown
     * topic (e.g. a transactional commit that merged an end offset for a topic never
     * registered by {@code onSend}): those are no-ops.
     *
     * @param topic the resolved topic
     */
    public void completeSend(String topic) {
        pendingSends.decrement(topic);
    }

    // ----------------------------------------------------------------
    // Subscription registration
    // ----------------------------------------------------------------

    /**
     * Registers the exact topic subscription of a group.
     *
     * @param groupId the consumer group
     * @param topics the subscribed topics
     */
    public void registerExactTopics(String groupId, Collection<String> topics) {
        subscriptions.registerExact(groupId, topics);
    }

    /**
     * Registers the pattern subscription of a group.
     *
     * @param groupId the consumer group
     * @param pattern the subscribed topic pattern
     */
    public void registerPattern(String groupId, Pattern pattern) {
        subscriptions.registerPattern(groupId, pattern);
    }

    // ----------------------------------------------------------------
    // Consumer assignments / readiness
    // ----------------------------------------------------------------

    /**
     * Registers a constructed consumer with an unknown assignment, so the group it
     * belongs to is not considered ready before the consumer polls.
     *
     * @param groupId the consumer group
     * @param consumer the consumer instance
     */
    public void registerConsumer(String groupId, Object consumer) {
        assignments.register(groupId, consumer);
    }

    /**
     * Replaces the assignment of a consumer, used by the manual {@code assign()} path.
     *
     * @param groupId the consumer group
     * @param consumer the consumer instance
     * @param assignment the partitions currently assigned to the consumer
     */
    public void recordConsumerAssignment(String groupId, Object consumer, Set<TopicPartition> assignment) {
        assignments.assign(groupId, consumer, assignment);
    }

    /**
     * Adds partitions reported as newly assigned by a rebalance listener callback.
     *
     * @param groupId the consumer group
     * @param consumer the consumer instance
     * @param partitions the newly assigned partitions
     */
    public void addConsumerPartitions(String groupId, Object consumer, Collection<TopicPartition> partitions) {
        assignments.addAll(groupId, consumer, partitions);
    }

    /**
     * Removes partitions reported as revoked or lost by a rebalance listener callback.
     *
     * @param groupId the consumer group
     * @param consumer the consumer instance
     * @param partitions the revoked or lost partitions
     */
    public void removeConsumerPartitions(String groupId, Object consumer, Collection<TopicPartition> partitions) {
        assignments.removeAll(groupId, consumer, partitions);
    }

    /**
     * Forgets the recorded assignment of a consumer, e.g. after it unsubscribed.
     *
     * @param groupId the consumer group
     * @param consumer the consumer instance
     */
    public void clearConsumerAssignment(String groupId, Object consumer) {
        assignments.clear(groupId, consumer);
    }

    /**
     * Removes a closed consumer from the assignment registry.
     *
     * @param groupId the consumer group
     * @param consumer the closed consumer instance
     */
    public void removeConsumerAssignment(String groupId, Object consumer) {
        assignments.remove(groupId, consumer);
    }

    /** {@code true} if the group has a registered subscription. */
    public boolean hasGroup(String groupId) {
        return subscriptions.hasGroup(groupId);
    }

    /** All groups that currently have at least one subscription. */
    public Set<String> knownGroupIds() {
        return subscriptions.groupIds();
    }

    /** {@code true} if the group has consumers and all of them have polled. */
    public boolean isGroupStable(String groupId) {
        return assignments.isStable(groupId);
    }

    /** {@code true} if the group is stable and every consumer has partitions. */
    public boolean isGroupReady(String groupId) {
        return assignments.isReady(groupId);
    }

    /** Number of consumers of the group registered for assignment tracking. */
    public int groupConsumerCount(String groupId) {
        return assignments.consumerCount(groupId);
    }

    /** Number of consumers of the group with a non-empty assignment. */
    public int groupAssignedCount(String groupId) {
        return assignments.assignedCount(groupId);
    }

    // ----------------------------------------------------------------
    // Consumer liveness
    // ----------------------------------------------------------------

    /**
     * Counts one more active consumer for the group.
     *
     * @param groupId the consumer group
     */
    public void incrementActiveConsumer(String groupId) {
        liveness.increment(groupId);
    }

    /**
     * Registers the shutdown of one consumer instance.
     *
     * <p>Idempotent: once a group's count reaches zero it is removed, and further
     * calls are no-ops. This tolerates Kafka's idempotent {@code close()}, but the
     * count must be decremented exactly once per {@code close()} call &mdash; a
     * single shutdown path that fires twice would prematurely drop groups that
     * still have live members (e.g. concurrency &gt; 1).
     */
    public void decrementActiveConsumer(String groupId) {
        if (liveness.decrement(groupId)) {
            subscriptions.removeGroup(groupId);
            offsets.removeGroup(groupId);
            assignments.removeGroup(groupId);
        }
    }

    // ----------------------------------------------------------------
    // Transactional path
    // ----------------------------------------------------------------

    /**
     * Buffers the acknowledged end offset of one transactional send for later
     * flush on commit.
     *
     * @param transactionalId the producer's {@code transactional.id}
     * @param tp the acknowledged topic partition
     * @param offset the acknowledged end offset
     */
    public void recordPendingEndOffset(String transactionalId, TopicPartition tp, long offset) {
        transactionTracker.record(transactionalId, tp, offset);
    }

    /**
     * Buffers the consumer-group offsets of one transactional offset commit for
     * later flush on commit.
     *
     * @param transactionalId the producer's {@code transactional.id}
     * @param groupId the consumer group whose offsets are being committed
     * @param offsets next-to-consume offsets supplied to
     *                {@code sendOffsetsToTransaction}
     */
    public void recordPendingGroupOffsets(String transactionalId, String groupId,
                                          Map<TopicPartition, OffsetAndMetadata> offsets) {
        transactionTracker.recordGroupOffsets(transactionalId, groupId, offsets);
    }

    /**
     * Flushes the buffered offsets of one producer: acknowledged message end
     * offsets and consumer-group offsets of transactional offset commits.
     * Called from the successful {@code commitTransaction} path.
     *
     * <p>Group offsets are recorded as-is (via {@link #onCommit}), because
     * {@code sendOffsetsToTransaction} already receives next-to-consume
     * positions; the {@code +1} conversion applies to message offsets only.
     *
     * @param transactionalId the producer's {@code transactional.id}
     */
    public void commitPendingEndOffsets(String transactionalId) {
        transactionTracker.take(transactionalId)
                .forEach(this::recordEndOffset);
        transactionTracker.takeGroupOffsets(transactionalId)
                .forEach((groupId, byPartition) ->
                        byPartition.forEach((tp, offset) -> onCommit(groupId, tp, offset)));
    }

    /**
     * Discards the buffered offsets of one producer: message end offsets and
     * consumer-group offsets. Called on abort and on producer close without
     * commit/abort.
     *
     * @param transactionalId the producer's {@code transactional.id}
     */
    public void discardPendingEndOffsets(String transactionalId) {
        transactionTracker.discard(transactionalId);
    }

    // ----------------------------------------------------------------
    // Lag calculation
    // ----------------------------------------------------------------

    /**
     * Returns whether the cluster currently has in-flight lag.
     *
     * @return {@code true} if pending sends or unconsumed offsets remain
     */
    public boolean hasLag() {
        return lag.hasLag();
    }

    private Set<String> getAllGroupIds() {
        return subscriptions.groupIds();
    }

    // ----------------------------------------------------------------
    // Partitions
    // ----------------------------------------------------------------

    /**
     * Returns the partitions observed on the cluster so far.
     *
     * @return the seen partitions
     */
    public Set<TopicPartition> getSeenPartitions() {
        return offsets.seenPartitions();
    }

    /**
     * Returns a human-readable description of the cluster state: groups with their
     * liveness and the offset summary.
     *
     * @return the description
     */
    public String describe() {
        return "groups: [" + getAllGroupIds().stream()
                .map(g -> g + "=alive:" + liveness.count(g)
                        + "/aliveFlag:" + liveness.isAlive(g))
                .sorted()
                .collect(Collectors.joining(", "))
                + "], " + offsets.describe();
    }

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    /**
     * Clears only the per-topic in-flight send counts, leaving the subscription
     * pattern cache (and all broker-derived state) intact.
     *
     * <p>Used for coarse recovery when a producer send fails before the broker
     * assigned topic/partition, so no topic is known to resolve the count: any
     * pending counts on the connection are suspect and are dropped rather than
     * leaking a forever-positive {@link #hasLag()}.
     *
     * @return {@code true} if anything was cleared, {@code false} if there was no
     *         pending state (e.g. the failure happened before the send operation was registered)
     */
    public boolean clearPendingTopics() {
        return pendingSends.clear();
    }

    /**
     * Clears the transient per-test state: pending send counts, the subscription
     * pattern cache and pending transactional offsets.
     */
    public void clear() {
        pendingSends.clear();
        subscriptions.clearPatternCache();
        transactionTracker.clear();
    }
}

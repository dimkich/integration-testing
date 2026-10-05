package io.github.dimkich.integration.testing.kafka.inflight.ledger;

import io.github.dimkich.integration.testing.kafka.util.BootstrapUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.DisposableBean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Central registry for tracking in-flight Kafka messages across producer sends
 * and consumer commits. Uses a static map keyed by normalized bootstrap server
 * addresses so that interceptors (created by Kafka, not Spring) can always find
 * the correct cluster state.
 *
 * <p><b>Lifecycle:</b> Each Spring context creates one bean per configured connection.
 * The bean owns a set of bootstrap keys and reference-counts them. When the context
 * closes ({@link #destroy()}), owned keys are released; the cluster entry is removed
 * only when no other context still owns it. This allows the ledger to survive context
 * resets in UntilStopped/loop mode (same broker, same keys) while reclaiming memory
 * from dead brokers across {@code @DirtiesContext} boundaries.
 *
 * <p><b>Static data path:</b> Interceptors call static methods
 * ({@link #onSend}, {@link #recordEndOffset}, {@link #onCommit}) which create
 * entries via {@code computeIfAbsent}. {@link #completeSend} is the matching
 * resolve path and never creates an entry. These entries are not reference-counted;
 * they are cleaned up when the owning bean's {@link #destroy()} releases the key.
 *
 * <p><b>Per-connection reset:</b> {@link #reset(String)} clears a single cluster,
 * scoped to the calling {@link InFlightLedgerScope}'s connection.
 *
 * @see ClusterState
 * @see InFlightLedgerScope
 */
@Slf4j
public class InFlightLedger implements DisposableBean {

    private static final ConcurrentHashMap<String, ClusterState> clusters = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> clusterRefs = new ConcurrentHashMap<>();

    /**
     * Producer/consumer identity registry. Static because advice methods call
     * the {@code handleXxx(...)} entry points statically.
     */
    private static final ClientRegistry clientRegistry = new ClientRegistry();

    private final Set<String> ownedKeys;

    /**
     * Creates a ledger that owns the given bootstrap server keys.
     * Each key is acquired (reference-counted) on construction.
     *
     * @param ownedKeys normalized bootstrap server addresses that this context owns
     */
    public InFlightLedger(Collection<String> ownedKeys) {
        this.ownedKeys = Set.copyOf(ownedKeys);
        this.ownedKeys.forEach(InFlightLedger::acquire);
    }

    // ----------------------------------------------------------------
    // Instance API (Spring bean)
    // ----------------------------------------------------------------

    /**
     * Clears the transient per-test state of a single cluster: pending send
     * counts, the subscription pattern cache, and the cluster's pending
     * transactional offsets.
     *
     * <p>{@link TransactionTracker} lives in {@link ClusterState}, so this reset
     * affects only the named cluster and reclaims pending offsets orphaned by
     * producers that were closed or garbage-collected without commit/abort; at
     * a test boundary there should be no open transaction, so the clear is safe.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} of the connection
     */
    public void reset(String bootstrapServers) {
        String key = BootstrapUtil.normalize(bootstrapServers);
        if (key == null) {
            return;
        }
        ClusterState state = clusters.get(key);
        if (state != null) {
            state.clear();
        }
    }

    @Override
    public void destroy() {
        ownedKeys.forEach(InFlightLedger::release);
    }

    /**
     * Returns whether any topic of the cluster has pending in-flight sends.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} of the connection
     * @return {@code true} if in-flight lag is present
     */
    public boolean hasLag(String bootstrapServers) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        if (normalized == null) {
            return false;
        }
        ClusterState state = clusters.get(normalized);
        return state != null && state.hasLag();
    }

    /**
     * Returns all non-internal partitions observed on the cluster.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} of the connection
     * @return the observed partitions, empty when the cluster is unknown
     */
    public Set<TopicPartition> getAllPartitions(String bootstrapServers) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        if (normalized == null) {
            return Collections.emptySet();
        }
        ClusterState state = clusters.get(normalized);
        if (state == null) {
            return Collections.emptySet();
        }
        return state.getSeenPartitions().stream()
                .filter(tp -> !tp.topic().startsWith("__"))
                .collect(Collectors.toSet());
    }

    /**
     * Returns a human-readable description of the cluster state for logs and timeout
     * messages.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} of the connection
     * @return the description, or {@code no-cluster}/{@code no-state} markers
     */
    public String describe(String bootstrapServers) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        if (normalized == null) {
            return "no-cluster";
        }
        ClusterState state = clusters.get(normalized);
        if (state == null) {
            return "no-state";
        }
        return state.describe();
    }

    /**
     * Returns whether every expected consumer group is ready to consume, based on
     * the assignment snapshots of the instrumented consumers. Unknown groups (no
     * known consumer on this cluster) are not ready.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} of the connection
     * @param expectedGroups consumer groups that must be ready; empty is always ready
     * @return {@code true} when every expected group has active consumers with
     *         non-empty assignments
     */
    public boolean areExpectedGroupsReady(String bootstrapServers, Set<String> expectedGroups) {
        if (expectedGroups.isEmpty()) {
            return true;
        }
        ClusterState state = clusterOrNull(bootstrapServers);
        if (state == null) {
            return false;
        }
        for (String groupId : expectedGroups) {
            if (!state.hasGroup(groupId) || !state.isGroupReady(groupId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether all known consumer groups are stable and every expected group
     * is present on this cluster.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} of the connection
     * @param expectedGroups consumer groups that must exist
     * @return {@code true} when all known groups are stable and all expected groups
     *         are present
     */
    public boolean areAllConsumerGroupsStable(String bootstrapServers, Set<String> expectedGroups) {
        ClusterState state = clusterOrNull(bootstrapServers);
        if (state == null) {
            return expectedGroups.isEmpty();
        }
        for (String groupId : expectedGroups) {
            if (!state.hasGroup(groupId)) {
                return false;
            }
        }
        for (String groupId : state.knownGroupIds()) {
            if (!state.isGroupStable(groupId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a comma-separated readiness report of the expected groups, used in
     * logs and timeout messages.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} of the connection
     * @param expectedGroups consumer groups to describe
     * @return entries in the form {@code groupId=ready},
     *         {@code groupId=STARTING(consumers=N, assigned=M)} or
     *         {@code groupId=missing}; {@code none} for an empty set
     */
    public String describeExpectedGroups(String bootstrapServers, Set<String> expectedGroups) {
        if (expectedGroups.isEmpty()) {
            return "none";
        }
        ClusterState state = clusterOrNull(bootstrapServers);
        return expectedGroups.stream()
                .map(groupId -> groupId + "=" + describeGroup(state, groupId))
                .collect(Collectors.joining(", "));
    }

    /**
     * Returns a comma-separated list of the known consumer group states of this
     * cluster, used in log messages.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} of the connection
     * @return entries in the form {@code groupId=READY}, {@code groupId=STABLE} or
     *         {@code groupId=STARTING}; {@code none} when no groups are known
     */
    public String describeGroupStates(String bootstrapServers) {
        ClusterState state = clusterOrNull(bootstrapServers);
        if (state == null) {
            return "none";
        }
        return state.knownGroupIds().stream()
                .sorted()
                .map(groupId -> groupId + "=" + (state.isGroupReady(groupId) ? "READY"
                        : state.isGroupStable(groupId) ? "STABLE" : "STARTING"))
                .collect(Collectors.joining(", "));
    }

    private ClusterState clusterOrNull(String bootstrapServers) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        return normalized == null ? null : clusters.get(normalized);
    }

    private static String describeGroup(ClusterState state, String groupId) {
        if (state == null || !state.hasGroup(groupId)) {
            return "missing";
        }
        if (state.isGroupReady(groupId)) {
            return "ready";
        }
        return "STARTING(consumers=" + state.groupConsumerCount(groupId)
                + ", assigned=" + state.groupAssignedCount(groupId) + ")";
    }

    // ----------------------------------------------------------------
    // Static API — Interceptors (data path)
    // ----------------------------------------------------------------

    /**
     * Registers a producer send in the cluster ledger.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} string
     * @param topic the target topic
     * @param partition the explicit partition, or {@code null}
     */
    public static void onSend(String bootstrapServers, String topic, Integer partition) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        if (normalized == null) {
            return;
        }
        getCluster(normalized).onSend(topic, partition);
    }

    /**
     * Records a consumer offset commit for a topic partition.
     *
     * <p>Called by the injected consumer interceptor on every commit. This is a
     * state-creating path ({@link #getCluster}): the cluster entry is created on
     * first contact, so a commit arriving before any producer send (consumer-only
     * scenario) is never dropped. A dropped commit would otherwise surface as
     * false lag (endOffset &gt; committed=0) until the next commit arrives.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} string
     * @param groupId consumer group id; skipped when null to avoid a
     *                {@code ConcurrentHashMap} null-key NPE downstream
     * @param tp committed topic partition
     * @param offset committed offset
     */
    public static void onCommit(String bootstrapServers, String groupId, TopicPartition tp, long offset) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        if (normalized == null) {
            return;
        }
        if (groupId == null) {
            log.warn("Consumer commit advice: groupId is null for bootstrap [{}]", normalized);
            return;
        }
        getCluster(normalized).onCommit(groupId, tp, offset);
    }

    /**
     * Records the broker end offset of a partition.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} string
     * @param tp the observed topic partition
     * @param offset the acknowledged offset
     */
    public static void recordEndOffset(String bootstrapServers, TopicPartition tp, long offset) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        if (normalized == null) {
            return;
        }
        getCluster(normalized).recordEndOffset(tp, offset);
    }

    /**
     * Resolves one in-flight send for a topic (decrements its pending count). Called
     * from the non-transactional producer acknowledgement path on both success and
     * failure, so a failed send does not leave its topic permanently pending.
     *
     * <p>Uses {@link #clusters} directly (no creation): the matching {@link #onSend}
     * already created the cluster entry, and a resolution for an unknown entry is a
     * legitimate no-op.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} string
     * @param topic the topic of the resolved send
     */
    public static void completeSend(String bootstrapServers, String topic) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        if (normalized == null) {
            return;
        }
        ClusterState state = clusters.get(normalized);
        if (state != null) {
            state.completeSend(topic);
        }
    }

    /**
     * Coarse recovery from a producer send failure that happened before the broker
     * assigned topic/partition: no topic is known on that path, so per-topic
     * resolution is impossible and all pending counts for the connection are
     * dropped instead of leaking a forever-positive lag check.
     *
     * <p>Uses {@link #clusters} directly (no creation), mirroring
     * {@link #completeSend}.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} string
     * @return {@code true} if any pending in-flight state was cleared
     */
    public static boolean clearPendingTopics(String bootstrapServers) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        if (normalized == null) {
            return false;
        }
        ClusterState state = clusters.get(normalized);
        return state != null && state.clearPendingTopics();
    }

    // ----------------------------------------------------------------
    // Static API — Interceptors (transactional path)
    // ----------------------------------------------------------------

    /**
     * Buffers the acknowledged end offset of one transactional send.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} string
     * @param transactionalId the producer's {@code transactional.id}
     * @param tp the acknowledged topic partition
     * @param offset the acknowledged end offset
     */
    public static void recordPendingEndOffset(String bootstrapServers, String transactionalId,
                                              TopicPartition tp, long offset) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        if (normalized == null) {
            return;
        }
        getCluster(normalized).recordPendingEndOffset(transactionalId, tp, offset);
    }

    /**
     * Discards the buffered transactional offsets of one producer. Wired to the
     * producer interceptor's {@code close()} hook so a producer closed without
     * commit/abort does not leak its pending offsets.
     *
     * @param bootstrapServers raw {@code bootstrap.servers} string
     * @param transactionalId the producer's {@code transactional.id}
     */
    public static void discardPendingEndOffsets(String bootstrapServers, String transactionalId) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        if (normalized == null) {
            return;
        }
        ClusterState state = clusters.get(normalized);
        if (state != null) {
            state.discardPendingEndOffsets(transactionalId);
        }
    }

    // ----------------------------------------------------------------
    // Static API — Thin Advices (control path)
    // ----------------------------------------------------------------

    /**
     * Returns a copy of the producer config with the in-flight interceptor
     * registered in {@code interceptor.classes}. The original config is not
     * modified; see {@link InterceptorInjector} for the rationale.
     *
     * <p>Called from the enter advice on {@code KafkaProducer} constructors, where
     * the return value replaces the instrumented constructor's first argument.
     *
     * @param config the config argument supplied to the producer constructor
     * @return a new config instance (or the input unchanged if it is not a Map/Properties)
     */
    public static Object prepareProducerConfig(Object config) {
        return InterceptorInjector.prepareProducerConfig(config);
    }

    /**
     * Registers a newly constructed producer. The producer's bootstrap servers and
     * {@code transactional.id} are read back from the constructor arguments,
     * which after the enter advice point at the interceptor-augmented copy.
     */
    public static void handleProducerConstructorExit(Object producer, Object[] args) {
        ClientConfigParser.ProducerCfg cfg = ClientConfigParser.parseProducerConfig(args);
        if (cfg.getBootstrapServers() != null) {
            clientRegistry.registerProducer(producer, cfg.getBootstrapServers(), cfg.getTransactionalId());
        }
    }

    /**
     * Returns a copy of the consumer config with the in-flight interceptor
     * registered in {@code interceptor.classes}. The original config is not
     * modified; see {@link InterceptorInjector} for the rationale.
     *
     * <p>Called from the enter advice on {@code KafkaConsumer} constructors, where
     * the return value replaces the instrumented constructor's first argument.
     *
     * @param config the config argument supplied to the consumer constructor
     * @return a new config instance (or the input unchanged if it is not a Map/Properties)
     */
    public static Object prepareConsumerConfig(Object config) {
        return InterceptorInjector.prepareConsumerConfig(config);
    }

    /**
     * Registers a newly constructed consumer and counts it as active for its consumer
     * group. Called from the exit advice on {@code KafkaConsumer} constructors.
     *
     * @param consumer the constructed consumer instance
     * @param args the constructor arguments, used to read the client configuration
     */
    public static void handleConsumerConstructorExit(Object consumer, Object[] args) {
        if (!clientRegistry.activate(consumer)) {
            return;  // intermediate constructor in the chain — already counted
        }
        ClientConfigParser.ConsumerCfg cfg = ClientConfigParser.parseConsumerConfig(args);
        if (cfg.getBootstrapServers() == null) {
            log.warn("Consumer constructor advice: bootstrap servers not found in config");
            return;
        }
        if (cfg.getGroupId() == null) {
            log.warn("Consumer constructor advice: groupId is null, bootstrap [{}]", cfg.getBootstrapServers());
        }
        clientRegistry.registerConsumer(consumer, cfg.getBootstrapServers(), cfg.getGroupId());
        incrementActiveConsumer(cfg.getBootstrapServers(), cfg.getGroupId(), consumer);
    }

    /**
     * Unregisters a closed consumer and decrements its group's active count. Called
     * from the exit advice on {@code KafkaConsumer.close()}.
     *
     * @param consumer the closed consumer instance
     */
    public static void handleConsumerClose(Object consumer) {
        if (!clientRegistry.deactivate(consumer)) {
            return;  // already closed — idempotent close
        }
        ConsumerIdentity id = clientRegistry.consumer(consumer);
        decrementActiveConsumer(id.bootstrapServers(), id.groupId());
        ClusterState state = clusters.get(id.bootstrapServers());
        if (state != null && id.groupId() != null) {
            state.removeConsumerAssignment(id.groupId(), consumer);
        }
    }

    /**
     * Registers the exact topic subscription of a consumer group. Called from the exit
     * advice on {@code KafkaConsumer.subscribe(Collection, ConsumerRebalanceListener)},
     * so a failed subscribe is not registered.
     *
     * @param consumer the subscribing consumer
     * @param topics the subscribed topics
     */
    public static void handleConsumerSubscribeExactTopics(Object consumer, Collection<String> topics) {
        ConsumerIdentity id = clientRegistry.consumer(consumer);
        if (id.groupId() == null) {
            log.warn("subscribe(Collection) advice: groupId is null");
            return;
        }
        getCluster(id.bootstrapServers()).registerExactTopics(id.groupId(), topics);
    }

    /**
     * Registers the pattern subscription of a consumer group. Called from the exit
     * advice on {@code KafkaConsumer.subscribe(Pattern, ConsumerRebalanceListener)},
     * so a failed subscribe is not registered.
     *
     * @param consumer the subscribing consumer
     * @param pattern the subscribed topic pattern
     */
    public static void handleConsumerSubscribePattern(Object consumer, Pattern pattern) {
        ConsumerIdentity id = clientRegistry.consumer(consumer);
        if (id.groupId() == null) {
            log.warn("subscribe(Pattern) advice: groupId is null, pattern [{}]", pattern);
            return;
        }
        getCluster(id.bootstrapServers()).registerPattern(id.groupId(), pattern);
    }

    /**
     * Registers the topics of manually assigned partitions as an exact subscription.
     * Called from the advice on {@code KafkaConsumer.assign(Collection)}.
     *
     * @param consumer the consumer being assigned
     * @param partitions the assigned partitions
     */
    public static void handleConsumerAssign(Object consumer, Collection<TopicPartition> partitions) {
        ConsumerIdentity id = clientRegistry.consumer(consumer);
        if (id.groupId() == null) {
            log.warn("assign(Collection) advice: groupId is null");
            return;
        }
        Set<String> topics = partitions.stream()
                .map(TopicPartition::topic)
                .collect(Collectors.toSet());
        ClusterState state = getCluster(id.bootstrapServers());
        state.registerExactTopics(id.groupId(), topics);
        state.recordConsumerAssignment(id.groupId(), consumer, new HashSet<>(partitions));
    }

    /**
     * Wraps the rebalance listener supplied to {@code KafkaConsumer.subscribe(...)} so
     * that assignment changes are reported to the ledger before they are delegated.
     * Called from the enter advice on the two-argument subscribe overloads.
     *
     * <p>A {@code null} listener is returned as is: Kafka's
     * {@code SubscriptionState.registerRebalanceListener} rejects null, and replacing
     * it with a wrapper would silently change that behavior. The no-argument subscribe
     * overloads pass Kafka's own no-op listener, which is wrapped like any other.
     *
     * @param consumer the subscribing consumer
     * @param listener the listener supplied by the application, may be {@code null}
     * @return the wrapper, or the input unchanged when it is {@code null} or already
     *         a wrapper
     */
    public static ConsumerRebalanceListener wrapRebalanceListener(Object consumer,
                                                                  ConsumerRebalanceListener listener) {
        if (listener == null || listener instanceof InFlightRebalanceListener) {
            return listener;
        }
        return new InFlightRebalanceListener(consumer, listener);
    }

    /**
     * Reports newly assigned partitions of a rebalance. Called before the application's
     * listener callback; cooperative rebalancing passes only the added partitions.
     *
     * @param consumer the consumer whose assignment changed
     * @param partitions the newly assigned partitions
     */
    public static void handleConsumerPartitionsAssigned(Object consumer, Collection<TopicPartition> partitions) {
        handleConsumerPartitionsChanged(consumer, partitions, true);
    }

    /**
     * Reports revoked partitions of a rebalance. With cooperative rebalancing the list
     * may be partial: the consumer can still own the remaining partitions.
     *
     * @param consumer the consumer whose assignment changed
     * @param partitions the revoked partitions
     */
    public static void handleConsumerPartitionsRevoked(Object consumer, Collection<TopicPartition> partitions) {
        handleConsumerPartitionsChanged(consumer, partitions, false);
    }

    /**
     * Reports partitions lost without a graceful revocation (session timeout, fencing).
     *
     * @param consumer the consumer whose assignment changed
     * @param partitions the lost partitions
     */
    public static void handleConsumerPartitionsLost(Object consumer, Collection<TopicPartition> partitions) {
        handleConsumerPartitionsChanged(consumer, partitions, false);
    }

    private static void handleConsumerPartitionsChanged(Object consumer, Collection<TopicPartition> partitions,
                                                        boolean assigned) {
        ConsumerIdentity id = clientRegistry.consumer(consumer);
        if (id.groupId() == null) {
            return;
        }
        ClusterState state = getCluster(id.bootstrapServers());
        if (assigned) {
            state.addConsumerPartitions(id.groupId(), consumer, partitions);
        } else {
            state.removeConsumerPartitions(id.groupId(), consumer, partitions);
        }
    }

    /**
     * Forgets the assignment of a consumer after {@code KafkaConsumer.unsubscribe()}.
     * Unlike rebalancing, unsubscribe clears the assignment without invoking the
     * rebalance listener, so the ledger must react to the call itself.
     *
     * @param consumer the unsubscribing consumer
     */
    public static void handleConsumerUnsubscribe(Object consumer) {
        ConsumerIdentity id = clientRegistry.consumer(consumer);
        if (id.groupId() == null) {
            return;
        }
        ClusterState state = clusters.get(id.bootstrapServers());
        if (state != null) {
            state.clearConsumerAssignment(id.groupId(), consumer);
        }
    }

    /**
     * Flushes or discards buffered transactional offsets after
     * {@code commitTransaction}. Called from the exit advice; when the commit failed,
     * the pending offsets are discarded.
     *
     * @param producer the producer that committed
     * @param t the exception thrown by the commit, or {@code null} on success
     */
    public static void handleTransactionCommit(Object producer, Throwable t) {
        ProducerIdentity id = clientRegistry.producer(producer);
        if (id.transactionalId() == null || id.bootstrapServers() == null) {
            return;
        }
        ClusterState state = clusters.get(id.bootstrapServers());
        if (state == null) {
            return;
        }
        if (t != null) {
            log.debug("Transaction failed for transactional.id [{}] on [{}], discarding pending offsets",
                    id.transactionalId(), id.bootstrapServers());
            state.discardPendingEndOffsets(id.transactionalId());
            return;
        }
        state.commitPendingEndOffsets(id.transactionalId());
    }

    /**
     * Discards buffered transactional offsets after {@code abortTransaction}. Called
     * from the exit advice.
     *
     * @param producer the producer that aborted
     */
    public static void handleTransactionAbort(Object producer) {
        ProducerIdentity id = clientRegistry.producer(producer);
        if (id.bootstrapServers() == null || id.transactionalId() == null) {
            return;
        }
        ClusterState state = clusters.get(id.bootstrapServers());
        if (state != null) {
            state.discardPendingEndOffsets(id.transactionalId());
        }
    }

    /**
     * Buffers the consumer-group offsets submitted with
     * {@code sendOffsetsToTransaction} for later flush on commit.
     *
     * <p>The producer's {@code transactional.id} is the buffer key and its
     * bootstrap servers select the cluster, so an unknown or non-transactional
     * producer is ignored (a non-transactional one cannot call this method
     * anyway). Offsets are recorded as-is: they are already next-to-consume
     * positions, unlike message offsets that need the {@code +1} treatment.
     *
     * @param producer the producing client instance
     * @param offsets consumer offsets supplied to the transactional commit
     * @param group the {@link ConsumerGroupMetadata} of the modern overload or
     *              the {@code String} group id of the deprecated one
     */
    public static void handleSendOffsetsToTransaction(Object producer, Map<TopicPartition, OffsetAndMetadata> offsets,
                                                      Object group) {
        if (offsets == null || offsets.isEmpty()) {
            return;
        }
        String groupId = resolveGroupId(group);
        if (groupId == null) {
            log.warn("sendOffsetsToTransaction advice: groupId is null");
            return;
        }
        ProducerIdentity id = clientRegistry.producer(producer);
        if (id.transactionalId() == null || id.bootstrapServers() == null) {
            return;
        }
        getCluster(id.bootstrapServers()).recordPendingGroupOffsets(id.transactionalId(), groupId, offsets);
    }

    /**
     * Extracts the consumer group id from either overload's group argument:
     * {@link ConsumerGroupMetadata} (modern) or {@link String} (deprecated).
     *
     * @return the group id, or {@code null} when the argument has an unexpected type
     */
    private static String resolveGroupId(Object group) {
        if (group instanceof ConsumerGroupMetadata metadata) {
            return metadata.groupId();
        }
        if (group instanceof String groupId) {
            return groupId;
        }
        return null;
    }

    // ----------------------------------------------------------------
    // Internal
    // ----------------------------------------------------------------

    static void incrementActiveConsumer(String bootstrapServers, String groupId, Object consumer) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        if (normalized == null) {
            log.warn("Consumer constructor advice: cannot normalize bootstrap servers [{}]", bootstrapServers);
            return;
        }
        if (groupId == null) {
            log.warn("Consumer constructor advice: groupId is null for bootstrap [{}]", normalized);
            return;
        }
        ClusterState state = getCluster(normalized);
        state.incrementActiveConsumer(groupId);
        state.registerConsumer(groupId, consumer);
    }

    static void decrementActiveConsumer(String bootstrapServers, String groupId) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        if (normalized == null || groupId == null) {
            return;
        }
        ClusterState state = clusters.get(normalized);
        if (state != null) {
            state.decrementActiveConsumer(groupId);
        }
    }

    /**
     * Returns the cluster state for a normalized bootstrap key, creating it on
     * first access. Interceptor data paths ({@link #onSend}, {@link #onCommit},
     * {@link #recordEndOffset}, consumer liveness) go through here so no event is
     * ever dropped; read-only/control paths use {@link #clusters} directly.
     */
    private static ClusterState getCluster(String normalizedBootstrap) {
        return clusters.computeIfAbsent(normalizedBootstrap, k -> new ClusterState());
    }

    private static void acquire(String key) {
        clusterRefs.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
    }

    private static void release(String key) {
        AtomicInteger refs = clusterRefs.get(key);
        if (refs != null && refs.decrementAndGet() == 0) {
            // ClusterState owns OffsetLedger, PendingSends, TopicSubscriptions,
            // ConsumerLiveness, and TransactionTracker — removing the entry
            // releases all of them.
            clusters.remove(key);
            clusterRefs.remove(key);
            // ClientRegistry is weak-based and self-cleans; explicit cleanup here
            // keeps destroy() deterministic rather than relying on GC timing.
            clientRegistry.removeByBootstrap(key);
        }
    }
}

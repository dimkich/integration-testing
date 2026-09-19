package io.github.dimkich.integration.testing.kafka.inflight.ledger;

import org.apache.kafka.common.TopicPartition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the observed assignment of every consumer instance, grouped by consumer
 * group. Used to answer the readiness questions of the wait completion without
 * contacting the broker: whether a group exists, whether all its consumers have
 * completed at least one rebalance, and whether all of them have partitions assigned.
 *
 * <p>An entry is created when the consumer is constructed and removed when it is
 * closed. The value is the partitions the consumer currently owns:
 * <ul>
 *   <li>{@code null} — no rebalance has completed yet, the assignment is unknown;</li>
 *   <li>empty set — the consumer owns no partitions (a completed rebalance with an
 *       empty assignment, or revoked everything);</li>
 *   <li>non-empty set — the consumer holds partitions.</li>
 * </ul>
 *
 * <p>The set is maintained incrementally: the rebalance listener reports newly
 * assigned partitions ({@link #addAll}) and revoked or lost partitions
 * ({@link #removeAll}); cooperative rebalancing reports both partially. A manual
 * {@code assign()} replaces the whole set ({@link #assign}).
 *
 * <p>A group is <b>stable</b> when all of its consumers have a known assignment
 * (a rebalance completed at least once) and <b>ready</b> when additionally every
 * assignment is non-empty — the in-memory equivalent of the Admin API
 * "running + assigned".
 *
 * <p>Maps are {@link WeakHashMap}-backed to mirror {@link ClientRegistry}: a
 * consumer that is garbage-collected without calling {@code close()} does not keep
 * this registry alive.
 *
 * <p>Package-private: detail of {@link ClusterState}, not part of the ledger's
 * public surface.
 */
final class ConsumerAssignments {

    private final Map<String, Map<Object, Set<TopicPartition>>> byGroup = new ConcurrentHashMap<>();

    /** Registers a constructed consumer with an unknown (no rebalance yet) assignment. */
    void register(String groupId, Object consumer) {
        consumersOf(groupId).put(consumer, null);
    }

    /** Replaces the assignment, used by the manual {@code assign()} path. */
    void assign(String groupId, Object consumer, Set<TopicPartition> assignment) {
        consumersOf(groupId).put(consumer, new HashSet<>(assignment));
    }

    /** Adds partitions reported as newly assigned by a rebalance. */
    void addAll(String groupId, Object consumer, Collection<TopicPartition> partitions) {
        change(groupId, consumer, partitions, true);
    }

    /** Removes partitions reported as revoked or lost by a rebalance. */
    void removeAll(String groupId, Object consumer, Collection<TopicPartition> partitions) {
        change(groupId, consumer, partitions, false);
    }

    /** Forgets the recorded assignment (e.g. after {@code unsubscribe()}). */
    void clear(String groupId, Object consumer) {
        Map<Object, Set<TopicPartition>> consumers = consumersOf(groupId);
        if (consumers.containsKey(consumer)) {
            consumers.put(consumer, null);
        }
    }

    /** Removes a closed consumer. */
    void remove(String groupId, Object consumer) {
        Map<Object, Set<TopicPartition>> consumers = byGroup.get(groupId);
        if (consumers != null) {
            synchronized (consumers) {
                consumers.remove(consumer);
            }
        }
    }

    /** Drops all state of a dead group. */
    void removeGroup(String groupId) {
        byGroup.remove(groupId);
    }

    /** {@code true} if the group has consumers and all of them completed a rebalance. */
    boolean isStable(String groupId) {
        Map<Object, Set<TopicPartition>> consumers = byGroup.get(groupId);
        if (consumers == null) {
            return false;
        }
        synchronized (consumers) {
            if (consumers.isEmpty()) {
                return false;
            }
            for (Set<TopicPartition> assignment : consumers.values()) {
                if (assignment == null) {
                    return false;
                }
            }
            return true;
        }
    }

    /** {@code true} if the group is stable and every consumer has partitions. */
    boolean isReady(String groupId) {
        Map<Object, Set<TopicPartition>> consumers = byGroup.get(groupId);
        if (consumers == null) {
            return false;
        }
        synchronized (consumers) {
            if (consumers.isEmpty()) {
                return false;
            }
            for (Set<TopicPartition> assignment : consumers.values()) {
                if (assignment == null || assignment.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    /** Number of live consumers of the group. */
    int consumerCount(String groupId) {
        Map<Object, Set<TopicPartition>> consumers = byGroup.get(groupId);
        if (consumers == null) {
            return 0;
        }
        synchronized (consumers) {
            return consumers.size();
        }
    }

    /** Number of consumers of the group with a non-empty assignment. */
    int assignedCount(String groupId) {
        Map<Object, Set<TopicPartition>> consumers = byGroup.get(groupId);
        if (consumers == null) {
            return 0;
        }
        synchronized (consumers) {
            return (int) consumers.values().stream()
                    .filter(assignment -> assignment != null && !assignment.isEmpty())
                    .count();
        }
    }

    private void change(String groupId, Object consumer, Collection<TopicPartition> partitions, boolean add) {
        Map<Object, Set<TopicPartition>> consumers = consumersOf(groupId);
        synchronized (consumers) {
            Set<TopicPartition> assignment = consumers.computeIfAbsent(consumer, k -> new HashSet<>());
            if (add) {
                assignment.addAll(partitions);
            } else {
                assignment.removeAll(partitions);
            }
        }
    }

    private Map<Object, Set<TopicPartition>> consumersOf(String groupId) {
        return byGroup.computeIfAbsent(groupId, k -> Collections.synchronizedMap(new WeakHashMap<>()));
    }
}

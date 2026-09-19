package io.github.dimkich.integration.testing.kafka.inflight.ledger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks, per consumer group, how many consumer instances are currently active.
 *
 * <p>A group is "alive" while its counter is positive. The counter is incremented
 * once per {@code KafkaConsumer} instance constructed for the group and decremented
 * once per {@code KafkaConsumer.close(Duration)} invocation. The idempotence of
 * the increment/decrement pairing is the caller's responsibility
 * (see {@code ClientRegistry.activate/deactivate}).
 *
 * <p>When the counter reaches zero, the group is removed. Callers that own
 * additional per-group state (subscriptions, committed offsets) are expected to
 * react to the {@code true} return of {@link #decrement(String)} by dropping that
 * state — the ledger must not keep stale groups around, otherwise
 * {@link OffsetLedger#describe()} and lag checks would see ghost subscriptions.
 *
 * <p>Package-private: detail of {@link ClusterState}, not part of the ledger's
 * public surface.
 */
final class ConsumerLiveness {

    private final ConcurrentHashMap<String, AtomicInteger> activeByGroup = new ConcurrentHashMap<>();

    void increment(String groupId) {
        activeByGroup.computeIfAbsent(groupId, k -> new AtomicInteger()).incrementAndGet();
    }

    /**
     * @return {@code true} if this decrement brought the group to zero and the
     *         group was removed; {@code false} otherwise (including unknown groups)
     */
    boolean decrement(String groupId) {
        AtomicInteger counter = activeByGroup.get(groupId);
        if (counter == null) {
            return false;
        }
        if (counter.decrementAndGet() <= 0) {
            activeByGroup.remove(groupId);
            return true;
        }
        return false;
    }

    boolean isAlive(String groupId) {
        AtomicInteger counter = activeByGroup.get(groupId);
        return counter != null && counter.get() > 0;
    }

    int count(String groupId) {
        AtomicInteger counter = activeByGroup.get(groupId);
        return counter == null ? 0 : counter.get();
    }
}
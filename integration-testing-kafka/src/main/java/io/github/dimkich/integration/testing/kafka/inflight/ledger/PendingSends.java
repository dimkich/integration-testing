package io.github.dimkich.integration.testing.kafka.inflight.ledger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-topic count of in-flight producer sends whose acknowledgement has not been
 * observed yet.
 *
 * <p>A counting map (not a set) because multiple messages can be in flight for the
 * same topic concurrently: each send increments and each acknowledgement decrements,
 * so the topic stays pending until the last in-flight send resolves.
 *
 * <p>Per-topic (not per-partition) because {@code ProducerInterceptor.onSend} runs
 * before partitioning and the partition is usually {@code null} there.
 *
 * <p>Package-private: this is a detail of {@link ClusterState}, not part of the
 * ledger's public surface.
 */
final class PendingSends {

    private final ConcurrentHashMap<String, AtomicInteger> byTopic = new ConcurrentHashMap<>();

    /** Registers one new in-flight send for {@code topic}. */
    void increment(String topic) {
        byTopic.computeIfAbsent(topic, k -> new AtomicInteger()).incrementAndGet();
    }

    /** Resolves one in-flight send for {@code topic}; unknown topics are no-ops. */
    void decrement(String topic) {
        byTopic.computeIfPresent(topic,
                (t, c) -> c.decrementAndGet() <= 0 ? null : c);
    }

    /** {@code true} if at least one send is still awaiting acknowledgement. */
    boolean hasPending() {
        return !byTopic.isEmpty();
    }

    /**
     * Drops all pending counts.
     *
     * @return {@code true} if anything was cleared, {@code false} if there was no
     *         pending state (mirrors the previous {@code ClusterState.clearPendingTopics()} contract)
     */
    boolean clear() {
        if (byTopic.isEmpty()) {
            return false;
        }
        byTopic.clear();
        return true;
    }

    /** For TRACE logging in {@link ClusterState#hasLag()}. */
    @Override
    public String toString() {
        return byTopic.toString();
    }
}
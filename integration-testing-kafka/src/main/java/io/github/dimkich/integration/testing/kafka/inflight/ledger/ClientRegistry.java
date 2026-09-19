package io.github.dimkich.integration.testing.kafka.inflight.ledger;

import io.github.dimkich.integration.testing.kafka.util.BootstrapUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Tracks the identity of each live producer/consumer instance and which consumer
 * instances are currently active.
 *
 * <p>All maps are {@link WeakHashMap}-backed: Kafka clients are owned by the
 * application and may be GC'd without ever calling {@code close()}; a weak key
 * prevents the registry from keeping them alive indefinitely.
 *
 * <p>Identity is normalized at registration time (via {@link BootstrapUtil}), so
 * every later lookup sees the same key regardless of resolver order or
 * round-robin DNS. This mirrors the previous {@code ClientContextRegistry}
 * contract.
 *
 * <p>Unregistered producers/consumers return {@link #DEFAULT_CONNECTION} with
 * {@code null} for the second component — preserving the previous behaviour where
 * unknown clients fell into the {@code "default"} bucket.
 *
 * <p>Package-private: internal to the inflight ledger.
 */
final class ClientRegistry {

    private static final String DEFAULT_CONNECTION = "default";

    private final Map<Object, ProducerIdentity> producers =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Object, ConsumerIdentity> consumers =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final Set<Object> activeConsumers =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    void registerProducer(Object producer, String bootstrapServers, String transactionalId) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        if (producer != null && normalized != null) {
            producers.put(producer, new ProducerIdentity(normalized, transactionalId));
        }
    }

    /** @return identity of {@code producer}, or {@code (default, null)} if unknown */
    ProducerIdentity producer(Object producer) {
        if (producer == null) {
            return new ProducerIdentity(DEFAULT_CONNECTION, null);
        }
        ProducerIdentity id = producers.get(producer);
        return id != null ? id : new ProducerIdentity(DEFAULT_CONNECTION, null);
    }

    void registerConsumer(Object consumer, String bootstrapServers, String groupId) {
        String normalized = BootstrapUtil.normalize(bootstrapServers);
        if (consumer != null && normalized != null) {
            consumers.put(consumer, new ConsumerIdentity(normalized, groupId));
        }
    }

    /** @return identity of {@code consumer}, or {@code (default, null)} if unknown */
    ConsumerIdentity consumer(Object consumer) {
        if (consumer == null) {
            return new ConsumerIdentity(DEFAULT_CONNECTION, null);
        }
        ConsumerIdentity id = consumers.get(consumer);
        return id != null ? id : new ConsumerIdentity(DEFAULT_CONNECTION, null);
    }

    /**
     * @return {@code true} if this consumer transitioned from inactive to active
     *         (first chained constructor); {@code false} if already active
     */
    boolean activate(Object consumer) {
        return activeConsumers.add(consumer);
    }

    /**
     * @return {@code true} if this consumer transitioned from active to inactive
     *         (first close); {@code false} if already inactive (repeated close)
     */
    boolean deactivate(Object consumer) {
        return activeConsumers.remove(consumer);
    }

    /**
     * Removes all identity records belonging to a single cluster.
     *
     * <p>Called from {@code InFlightLedger.release} when the last owning Spring
     * context releases a cluster key. The backing maps are weak, so this is not
     * strictly required for correctness — GC would eventually drop entries whose
     * keys become unreachable — but it makes {@code destroy()} deterministic
     * rather than GC-timing dependent.
     *
     * <p>All iterations are guarded by the same mutex used by
     * {@code Collections.synchronizedMap/Set} for individual operations.
     *
     * @param normalizedBootstrap normalized bootstrap.servers of the cluster to clear
     */
    void removeByBootstrap(String normalizedBootstrap) {
        if (normalizedBootstrap == null) {
            return;
        }
        synchronized (producers) {
            producers.entrySet().removeIf(e ->
                    normalizedBootstrap.equals(e.getValue().bootstrapServers()));
        }
        synchronized (consumers) {
            consumers.entrySet().removeIf(e ->
                    normalizedBootstrap.equals(e.getValue().bootstrapServers()));
        }
        synchronized (activeConsumers) {
            activeConsumers.removeIf(c -> {
                ConsumerIdentity id = consumers.get(c);
                // Either the consumer was GC'd without close() (id == null) or
                // it belongs to the bootstrap being released — deactivate both.
                return id == null || normalizedBootstrap.equals(id.bootstrapServers());
            });
        }
    }
}
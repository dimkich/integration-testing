package io.github.dimkich.integration.testing.kafka.inflight.ledger;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;

/**
 * Rebalance listener wrapper injected into {@code KafkaConsumer.subscribe(...)}: reports
 * the partitions assigned, revoked and lost to the {@link InFlightLedger} and then
 * delegates to the listener supplied by the application.
 *
 * <p>The callbacks are invoked by Kafka when a rebalance completes (or when partitions
 * are taken away), inside {@code poll()} and before it starts fetching — this makes the
 * assignment visible to the wait completion immediately, without waiting for the poll
 * timeout and without depending on the application's listener implementation.
 *
 * <p>Package-private: created by {@link InFlightLedger#wrapRebalanceListener}.
 */
final class InFlightRebalanceListener implements ConsumerRebalanceListener {

    private final Object consumer;
    private final ConsumerRebalanceListener delegate;

    InFlightRebalanceListener(Object consumer, ConsumerRebalanceListener delegate) {
        this.consumer = consumer;
        this.delegate = delegate;
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        InFlightLedger.handleConsumerPartitionsAssigned(consumer, partitions);
        delegate.onPartitionsAssigned(partitions);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        InFlightLedger.handleConsumerPartitionsRevoked(consumer, partitions);
        delegate.onPartitionsRevoked(partitions);
    }

    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitions) {
        InFlightLedger.handleConsumerPartitionsLost(consumer, partitions);
        delegate.onPartitionsLost(partitions);
    }
}

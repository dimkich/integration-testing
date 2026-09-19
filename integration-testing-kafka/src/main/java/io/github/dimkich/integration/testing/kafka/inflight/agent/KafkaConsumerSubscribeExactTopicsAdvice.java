package io.github.dimkich.integration.testing.kafka.inflight.agent;

import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedger;
import net.bytebuddy.asm.Advice;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;

import java.util.Collection;

/**
 * Byte Buddy advice on {@code KafkaConsumer.subscribe(Collection, ConsumerRebalanceListener)}:
 * replaces the rebalance listener with a ledger-reporting wrapper and registers the exact
 * topic subscription once the call succeeded.
 */
public class KafkaConsumerSubscribeExactTopicsAdvice {

    /**
     * Entry advice method inlined by Byte Buddy. The listener argument is replaced with
     * a wrapper that reports assignment changes to the ledger and delegates to the
     * original listener.
     *
     * @param consumer the subscribing consumer
     * @param listener the listener supplied by the application
     */
    @SuppressWarnings({"unused", "UnusedAssignment"})
    @Advice.OnMethodEnter
    public static void onEnter(
            @Advice.This Object consumer,
            @Advice.Argument(value = 1, readOnly = false) ConsumerRebalanceListener listener) {
        listener = InFlightLedger.wrapRebalanceListener(consumer, listener);
    }

    /**
     * Exit advice method inlined by Byte Buddy; runs only on a successful subscribe.
     *
     * @param consumer the subscribed consumer
     * @param topics the subscribed topics
     */
    @SuppressWarnings("unused")
    @Advice.OnMethodExit
    public static void onExit(
            @Advice.This Object consumer,
            @Advice.Argument(0) Collection<String> topics) {
        InFlightLedger.handleConsumerSubscribeExactTopics(consumer, topics);
    }
}

package io.github.dimkich.integration.testing.kafka.inflight.agent;

import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedger;
import net.bytebuddy.asm.Advice;

/**
 * Byte Buddy advice on {@code KafkaConsumer.unsubscribe()}: drops the consumer's known
 * assignment, because Kafka clears it without invoking the rebalance listener.
 */
public class KafkaConsumerUnsubscribeAdvice {

    /**
     * Entry advice method inlined by Byte Buddy.
     *
     * @param consumer the unsubscribing consumer
     */
    @SuppressWarnings("unused")
    @Advice.OnMethodEnter
    public static void onUnsubscribe(@Advice.This Object consumer) {
        InFlightLedger.handleConsumerUnsubscribe(consumer);
    }
}

package io.github.dimkich.integration.testing.kafka.inflight.agent;

import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedger;
import net.bytebuddy.asm.Advice;

/**
 * Fired when {@code KafkaConsumer.close(Duration)} is invoked.
 *
 * <p>The agent intentionally instruments only {@code close(Duration)}, because
 * in Kafka 3.x this is the sole "terminal" overload: {@code close()} delegates
 * to {@code close(Duration)}, and the legacy {@code close(long, TimeUnit)} was
 * removed. Routing every public shutdown path through {@link #onClose} exactly
 * once keeps the per-group active-consumer count in sync with reality.
 *
 * <p>{@link InFlightLedger#handleConsumerClose} makes the accounting idempotent
 * for an external repeated {@code close()}: the first transition deactivates the
 * consumer and decrements the group count, any subsequent transition is a no-op.
 *
 * <p>See {@code KafkaInFlightAgent#configureBuilder} for the matching rationale.
 */
public class KafkaConsumerCloseAdvice {

    /**
     * Entry advice method inlined by Byte Buddy.
     *
     * @param consumer the consumer being closed
     */
    @SuppressWarnings("unused")
    @Advice.OnMethodEnter
    public static void onClose(@Advice.This Object consumer) {
        InFlightLedger.handleConsumerClose(consumer);
    }
}

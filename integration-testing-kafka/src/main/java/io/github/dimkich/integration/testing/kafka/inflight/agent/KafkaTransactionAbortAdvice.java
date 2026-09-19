package io.github.dimkich.integration.testing.kafka.inflight.agent;

import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedger;
import net.bytebuddy.asm.Advice;

/**
 * Cleans up the producer's buffered transactional offsets when
 * {@code KafkaProducer.abortTransaction()} returns, whether it completes normally or
 * exceptionally. {@code onThrowable = Throwable.class} makes the exit advice run on
 * exceptional exits too; otherwise a failed abort would leave the pending offsets in
 * the cluster's transaction tracker forever.
 */
public class KafkaTransactionAbortAdvice {

    /**
     * Exit advice method inlined by Byte Buddy.
     *
     * @param producer the producer whose transaction was aborted
     */
    @SuppressWarnings("unused")
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onAbortTransactionExit(@Advice.This Object producer) {
        InFlightLedger.handleTransactionAbort(producer);
    }
}
package io.github.dimkich.integration.testing.kafka.inflight.agent;

import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedger;
import net.bytebuddy.asm.Advice;

/**
 * Byte Buddy advice on {@code KafkaProducer.commitTransaction()}: flushes or discards the
 * buffered transactional offsets depending on the commit outcome.
 */
public class KafkaTransactionCommitAdvice {

    /**
     * Exit advice method inlined by Byte Buddy.
     *
     * @param producer the producer that committed
     * @param t the exception thrown by the commit, or {@code null} on success
     */
    @SuppressWarnings("unused")
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onCommitTransactionExit(
            @Advice.This Object producer,
            @Advice.Thrown Throwable t) {
        InFlightLedger.handleTransactionCommit(producer, t);
    }
}

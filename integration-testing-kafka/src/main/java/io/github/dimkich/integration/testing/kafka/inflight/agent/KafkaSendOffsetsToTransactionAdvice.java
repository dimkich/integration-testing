package io.github.dimkich.integration.testing.kafka.inflight.agent;

import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedger;
import net.bytebuddy.asm.Advice;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;

/**
 * Buffers the consumer-group offsets submitted with
 * {@code KafkaProducer.sendOffsetsToTransaction} until the enclosing transaction
 * commits or aborts.
 *
 * <p>Offsets passed to {@code sendOffsetsToTransaction} become visible to the
 * broker only when {@code commitTransaction} succeeds, so recording them
 * immediately would produce a false "no lag" for a transaction that is later
 * aborted. The matching flush/discard is done by the commit/abort advice, which
 * consumes the buffer created here.
 *
 * <p>{@code KafkaProducer} declares two overloads: the modern
 * {@code ConsumerGroupMetadata} one and the deprecated {@code String} one, which
 * delegates to the former. Matching by method name with a plain {@code Object}
 * group argument covers both without referencing a type that may be absent in
 * older or newer clients; the double invocation on the delegating path is
 * harmless because the buffer merge is idempotent.
 */
public class KafkaSendOffsetsToTransactionAdvice {

    /**
     * Buffers the offsets only when the call completed normally: a thrown
     * exception means the offsets were not accepted by the transaction, so there
     * is nothing to flush later.
     */
    @SuppressWarnings("unused")
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onSendOffsetsToTransactionExit(
            @Advice.This Object producer,
            @Advice.Argument(0) Map<TopicPartition, OffsetAndMetadata> offsets,
            @Advice.Argument(1) Object group,
            @Advice.Thrown Throwable t) {
        if (t != null) {
            return;
        }
        InFlightLedger.handleSendOffsetsToTransaction(producer, offsets, group);
    }
}

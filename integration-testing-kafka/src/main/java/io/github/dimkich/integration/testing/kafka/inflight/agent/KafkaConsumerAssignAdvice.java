package io.github.dimkich.integration.testing.kafka.inflight.agent;

import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedger;
import net.bytebuddy.asm.Advice;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;

/**
 * Byte Buddy advice on {@code KafkaConsumer.assign(Collection)}: registers the manually
 * assigned partitions with the in-flight ledger.
 */
public class KafkaConsumerAssignAdvice {

    /**
     * Entry advice method inlined by Byte Buddy.
     *
     * @param consumer the consumer instance
     * @param partitions the assigned partitions
     */
    @SuppressWarnings("unused")
    @Advice.OnMethodEnter
    public static void onAssign(
            @Advice.This Object consumer,
            @Advice.Argument(0) Collection<TopicPartition> partitions) {
        InFlightLedger.handleConsumerAssign(consumer, partitions);
    }
}

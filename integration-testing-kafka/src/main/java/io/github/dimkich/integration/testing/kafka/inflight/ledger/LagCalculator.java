package io.github.dimkich.integration.testing.kafka.inflight.ledger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;

/**
 * Stateless lag computation over the four ledger collaborators.
 *
 * <p>Two independent sources of lag are checked, in order:
 * <ol>
 *   <li><b>In-flight producer sends.</b> If any send has not been acknowledged
 *       yet, there may still be a message that has been produced but not yet
 *       consumed — no point in even looking at committed offsets.</li>
 *   <li><b>Unconsumed partitions of live groups.</b> For each live group and
 *       each end-offset entry whose topic the group subscribes to, compare the
 *       broker's end offset with the group's committed offset. Any positive
 *       delta is lag.</li>
 * </ol>
 *
 * <p>Dead groups (zero active consumers) are skipped: their committed offsets
 * are gone from {@link OffsetLedger} anyway, but the guard avoids a spurious
 * "end offset &gt; 0 = lag" for a group whose only consumer just shut down.
 *
 * <p>Package-private: not part of the ledger's public surface, exists only to
 * keep {@link ClusterState} free of the offset-comparison algorithm.
 */
@Slf4j
@RequiredArgsConstructor
final class LagCalculator {

    private final PendingSends pending;
    private final OffsetLedger offsets;
    private final TopicSubscriptions subscriptions;
    private final ConsumerLiveness liveness;

    /** {@code true} if the cluster has messages that have not been consumed. */
    boolean hasLag() {
        if (pending.hasPending()) {
            log.debug("Lag: pending acks for topics {}", pending);
            return true;
        }
        for (String groupId : subscriptions.groupIds()) {
            if (liveness.isAlive(groupId) && groupHasLag(groupId)) {
                return true;
            }
        }
        return false;
    }

    private boolean groupHasLag(String groupId) {
        for (Map.Entry<TopicPartition, Long> entry : offsets.endOffsetEntries()) {
            TopicPartition tp = entry.getKey();
            if (!subscriptions.isSubscribed(groupId, tp.topic())) {
                continue;
            }
            long committed = offsets.committedOffset(groupId, tp);
            if (entry.getValue() > committed) {
                log.debug("Lag: group [{}] [{}] end={} committed={}",
                        groupId, tp, entry.getValue(), committed);
                return true;
            }
        }
        return false;
    }
}
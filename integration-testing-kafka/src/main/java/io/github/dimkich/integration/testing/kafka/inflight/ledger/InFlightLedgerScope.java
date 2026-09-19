package io.github.dimkich.integration.testing.kafka.inflight.ledger;

import io.github.dimkich.integration.testing.kafka.KafkaStateChecker;
import io.github.dimkich.integration.testing.kafka.LagCheckResult;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Per-connection state checker that delegates to {@link InFlightLedger} for a
 * specific bootstrap server. Created by {@link io.github.dimkich.integration.testing.kafka.config.KafkaConfig}
 * during bean registration (one per configured connection when inflight mode is enabled).
 *
 * <p>{@link #reset()} clears only this connection's cluster, not all clusters.
 * This avoids the previous issue where one connection's reset would wipe state
 * belonging to other connections.
 *
 * @see InFlightLedger
 */
@RequiredArgsConstructor
public class InFlightLedgerScope implements KafkaStateChecker {
    private final InFlightLedger inFlightLedger;
    private final String bootstrapServers;

    @Override
    public void reset() {
        inFlightLedger.reset(bootstrapServers);
    }

    @Override
    public LagCheckResult checkLag() {
        return new LagCheckResult(
                inFlightLedger.hasLag(bootstrapServers),
                inFlightLedger.getAllPartitions(bootstrapServers));
    }

    @Override
    public String describe() {
        return inFlightLedger.describe(bootstrapServers);
    }

    @Override
    public boolean areExpectedGroupsReady(Set<String> expectedGroups) {
        return inFlightLedger.areExpectedGroupsReady(bootstrapServers, expectedGroups);
    }

    @Override
    public boolean areAllConsumerGroupsStable(Set<String> expectedGroups) {
        return inFlightLedger.areAllConsumerGroupsStable(bootstrapServers, expectedGroups);
    }

    @Override
    public String describeExpectedGroups(Set<String> expectedGroups) {
        return inFlightLedger.describeExpectedGroups(bootstrapServers, expectedGroups);
    }

    @Override
    public String describeGroupStates() {
        return inFlightLedger.describeGroupStates(bootstrapServers);
    }
}

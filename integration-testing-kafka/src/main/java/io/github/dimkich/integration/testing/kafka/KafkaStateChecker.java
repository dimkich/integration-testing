package io.github.dimkich.integration.testing.kafka;

import java.util.Set;

/**
 * Provides the Kafka state awaited by {@code KafkaWaitCompletion}: whether messages
 * produced to Kafka still have to be consumed and whether the consumer groups of the
 * system under test are ready.
 *
 * <p>Two implementations exist: the Admin API one asks the broker
 * ({@code KafkaAdminRepository}), the in-flight ledger one derives the same state from
 * instrumented clients in memory ({@code InFlightLedgerScope}). The wait completion
 * code uses the interface only, so both modes share one code path.</p>
 */
public interface KafkaStateChecker {
    /**
     * Resets previously accumulated state before a new wait cycle.
     */
    default void reset() {
    }

    /**
     * Computes the current lag state.
     *
     * @return the lag state together with the partitions examined
     */
    LagCheckResult checkLag();

    /**
     * Returns a human-readable description of the current checker state, used in logs
     * and timeout messages.
     *
     * @return the description
     */
    String describe();

    /**
     * Returns whether every expected consumer group is ready to consume: the group
     * exists, has active consumers and all of them have partitions assigned.
     *
     * @param expectedGroups consumer groups that must be ready; an empty set is
     *                       always ready
     * @return {@code true} when every expected group is ready
     */
    boolean areExpectedGroupsReady(Set<String> expectedGroups);

    /**
     * Returns whether all known consumer groups are stable and every expected group
     * is present. A missing expected group makes the result {@code false}.
     *
     * @param expectedGroups consumer groups that must exist
     * @return {@code true} when all known groups are stable and all expected groups
     *         are present
     */
    boolean areAllConsumerGroupsStable(Set<String> expectedGroups);

    /**
     * Returns a comma-separated readiness report of the expected groups, used in logs
     * and timeout messages.
     *
     * @param expectedGroups consumer groups to describe
     * @return the description; {@code none} for an empty set
     */
    String describeExpectedGroups(Set<String> expectedGroups);

    /**
     * Returns a comma-separated list of the known consumer group states, used in log
     * messages.
     *
     * @return the description
     */
    String describeGroupStates();
}

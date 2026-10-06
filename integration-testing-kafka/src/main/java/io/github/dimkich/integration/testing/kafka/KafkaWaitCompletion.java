package io.github.dimkich.integration.testing.kafka;

import io.github.dimkich.integration.testing.kafka.config.ConnectionProperties;
import io.github.dimkich.integration.testing.kafka.config.KafkaProperties;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * {@link WaitCompletion} implementation for Kafka: waits until the sniffer and all
 * live consumer groups have caught up (no lag remains), so all outbound and inbound
 * messages are captured before assertions run.
 *
 * <p>{@link #start()} only resets the tracking state and never blocks. Consumer groups
 * listed in {@code integration.testing.kafka.connections.<name>.expected-groups} are
 * awaited in {@link #waitCompletion()}: it first waits up to
 * {@code startup-stabilization-timeout-seconds} for the sniffer assignment and for the
 * expected groups to become ready and all known groups stable (covering SUT consumers
 * that start asynchronously, possibly triggered by test initialization), and only then
 * waits up to {@code lag-polling-timeout-ms} for the lag to drain. An empty group
 * listing alone is not treated as "nothing to wait for".
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaWaitCompletion implements WaitCompletion {
    private final KafkaSnifferConsumer sniffer;
    private final KafkaStateChecker stateChecker;
    private final KafkaProperties kafkaProperties;
    private volatile boolean anyLag = false;

    @Override
    public void start() {
        sniffer.setLastException(null);
        stateChecker.reset();
    }

    @Override
    public boolean isAnyTaskStarted() {
        return anyLag;
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("BusyWait")
    public void waitCompletion() {
        long readinessDeadlineNanos = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(kafkaProperties.getStartupStabilizationTimeoutSeconds());
        long lagDeadlineNanos = 0;
        anyLag = false;
        Set<String> expectedGroups = expectedGroups();
        long readinessRecheckNanos = TimeUnit.MILLISECONDS.toNanos(
                Math.max(kafkaProperties.getReadinessPollingIntervalMs(), kafkaProperties.getLagPollingIntervalMs()));
        long nextReadinessCheckNanos = 0;
        boolean snifferReady;
        boolean sutReady = false;

        while (true) {
            Exception snifferError = sniffer.getLastException();
            if (snifferError != null) {
                throw snifferError;
            }

            LagCheckResult result = stateChecker.checkLag();

            snifferReady = sniffer.getAssignment().containsAll(result.getPartitions());
            if (!snifferReady) {
                sniffer.signalMetadataNeedsRefresh();
            }

            long nowNanos = System.nanoTime();
            if (!sutReady && nowNanos >= nextReadinessCheckNanos) {
                sutReady = stateChecker.areExpectedGroupsReady(expectedGroups)
                        && (expectedGroups.isEmpty() || stateChecker.areAllConsumerGroupsStable(expectedGroups));
                nextReadinessCheckNanos = nowNanos + readinessRecheckNanos;
            }

            if (!snifferReady || !sutReady) {
                if (nowNanos >= readinessDeadlineNanos) {
                    break;
                }
                if (log.isTraceEnabled()) {
                    log.trace("WaitCompletion: not ready yet. sniffer assigned: {}, expected groups ready: {}",
                            sniffer.getAssignment().size(), sutReady);
                }
                Thread.sleep(kafkaProperties.getLagPollingIntervalMs());
                continue;
            }

            if (lagDeadlineNanos == 0) {
                lagDeadlineNanos = nowNanos + TimeUnit.MILLISECONDS.toNanos(kafkaProperties.getLagPollingTimeoutMs());
            }

            if (!result.isHasLag()) {
                if (log.isTraceEnabled()) {
                    log.trace("WaitCompletion: no lag. {}", stateChecker.describe());
                }
                return;
            }
            anyLag = true;

            if (nowNanos >= lagDeadlineNanos) {
                break;
            }

            Thread.sleep(kafkaProperties.getLagPollingIntervalMs());
        }
        Exception snifferError = sniffer.getLastException();
        if (snifferError != null) {
            throw new RuntimeException("Sniffer error on " + sniffer.getConnectionNames(), snifferError);
        }
        List<String> problems = new ArrayList<>();
        if (!snifferReady) {
            problems.add("sniffer is not assigned to all partitions, assigned: " + sniffer.getAssignment());
        }
        if (!sutReady && !expectedGroups.isEmpty()) {
            problems.add("expected consumer groups are not ready: "
                    + stateChecker.describeExpectedGroups(expectedGroups)
                    + ", groups: " + stateChecker.describeGroupStates());
        }
        if (problems.isEmpty()) {
            throw new RuntimeException("Kafka wait completion timeout. Unprocessed messages remaining on broker.");
        }
        throw new RuntimeException("Kafka wait completion timeout. " + String.join("; ", problems) + ".");
    }

    /**
     * Returns the union of {@code expected-groups} configured for all connections
     * served by this wait completion. Connections sharing one cluster contribute
     * their groups to the same set.
     *
     * @return the expected consumer groups, empty when none are configured
     */
    private Set<String> expectedGroups() {
        Set<String> groups = new LinkedHashSet<>();
        for (String connectionName : sniffer.getConnectionNames()) {
            ConnectionProperties connection = kafkaProperties.getConnection(connectionName);
            if (connection.getExpectedGroups() != null) {
                groups.addAll(connection.getExpectedGroups());
            }
        }
        return groups;
    }
}

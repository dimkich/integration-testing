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
 * <p>Consumer groups listed in
 * {@code integration.testing.kafka.connections.<name>.expected-groups} are awaited
 * explicitly: before each test ({@link #start()}) and on every poll iteration
 * ({@link #waitCompletion()}). This covers SUT consumers that start asynchronously
 * after the Spring context has been refreshed — an empty group listing alone is not
 * treated as "nothing to wait for" anymore.
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaWaitCompletion implements WaitCompletion {
    private final KafkaSnifferConsumer sniffer;
    private final KafkaStateChecker stateChecker;
    private final KafkaProperties kafkaProperties;
    private volatile boolean anyLag = false;

    @Override
    @SneakyThrows
    @SuppressWarnings("BusyWait")
    public void start() {
        sniffer.setLastException(null);
        Set<String> expectedGroups = expectedGroups();
        long checkIntervalNanos = TimeUnit.MILLISECONDS.toNanos(
                Math.max(kafkaProperties.getReadinessPollingIntervalMs(), kafkaProperties.getLagPollingIntervalMs()));
        long startedNanos = System.nanoTime();
        long deadlineNanos = startedNanos
                + TimeUnit.SECONDS.toNanos(kafkaProperties.getStartupStabilizationTimeoutSeconds());
        long nextCheckNanos = 0;
        boolean expectedReady = expectedGroups.isEmpty();
        while (System.nanoTime() < deadlineNanos) {
            if (System.nanoTime() >= nextCheckNanos) {
                expectedReady = stateChecker.areExpectedGroupsReady(expectedGroups);
                if (expectedReady && stateChecker.areAllConsumerGroupsStable(expectedGroups)) {
                    break;
                }
                nextCheckNanos = System.nanoTime() + checkIntervalNanos;
            }
            Thread.sleep(kafkaProperties.getLagPollingIntervalMs());
        }
        if (!expectedReady) {
            throw new IllegalStateException("Kafka startup stabilization timeout after "
                    + kafkaProperties.getStartupStabilizationTimeoutSeconds()
                    + "s: expected consumer groups are not ready ["
                    + stateChecker.describeExpectedGroups(expectedGroups) + "], all groups ["
                    + stateChecker.describeGroupStates() + "]. Check that the SUT listeners have started.");
        }
        if (log.isTraceEnabled()) {
            log.trace("Startup stabilization finished after {} ms, sniffer assignment: {}, groups: {}, state: {}",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos),
                    sniffer.getAssignment().size(),
                    stateChecker.describeGroupStates(),
                    stateChecker.describe());
        }
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
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(kafkaProperties.getLagPollingTimeoutMs());
        anyLag = false;
        Set<String> expectedGroups = expectedGroups();
        long readinessRecheckNanos = TimeUnit.MILLISECONDS.toNanos(
                Math.max(kafkaProperties.getReadinessPollingIntervalMs(), kafkaProperties.getLagPollingIntervalMs()));
        long nextReadinessCheckNanos = 0;
        boolean snifferReady = false;
        boolean sutReady = false;

        while (System.nanoTime() < deadlineNanos) {
            Exception snifferError = sniffer.getLastException();
            if (snifferError != null) {
                throw snifferError;
            }

            LagCheckResult result = stateChecker.checkLag();

            snifferReady = sniffer.getAssignment().containsAll(result.getPartitions());
            if (!snifferReady) {
                sniffer.signalMetadataNeedsRefresh();
            }

            if (!sutReady && System.nanoTime() >= nextReadinessCheckNanos) {
                sutReady = stateChecker.areExpectedGroupsReady(expectedGroups);
                nextReadinessCheckNanos = System.nanoTime() + readinessRecheckNanos;
            }

            if (!snifferReady || !sutReady) {
                if (log.isTraceEnabled()) {
                    log.trace("WaitCompletion: not ready yet. sniffer assigned: {}, expected groups ready: {}",
                            sniffer.getAssignment().size(), sutReady);
                }
                Thread.sleep(kafkaProperties.getLagPollingIntervalMs());
                continue;
            }

            boolean currentLag = result.isHasLag();
            if (currentLag) {
                anyLag = true;
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("WaitCompletion: no lag. {}", stateChecker.describe());
                }
                return;
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
                    + stateChecker.describeExpectedGroups(expectedGroups));
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

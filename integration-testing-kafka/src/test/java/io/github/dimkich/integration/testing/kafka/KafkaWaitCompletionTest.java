package io.github.dimkich.integration.testing.kafka;

import io.github.dimkich.integration.testing.kafka.config.ConnectionProperties;
import io.github.dimkich.integration.testing.kafka.config.KafkaProperties;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class KafkaWaitCompletionTest {

    private static final TopicPartition PARTITION = new TopicPartition("topic", 0);

    private KafkaSnifferConsumer sniffer;
    private KafkaStateChecker stateChecker;
    private KafkaProperties kafkaProperties;
    private InboundMessageRegistry inboundMessageRegistry;
    private KafkaWaitCompletion waitCompletion;

    @BeforeEach
    void setUp() {
        sniffer = mock(KafkaSnifferConsumer.class);
        stateChecker = mock(KafkaStateChecker.class);
        kafkaProperties = mock(KafkaProperties.class);
        inboundMessageRegistry = mock(InboundMessageRegistry.class);
        waitCompletion = new KafkaWaitCompletion(sniffer, stateChecker, kafkaProperties, inboundMessageRegistry);

        when(sniffer.getConnectionNames()).thenReturn(List.of("kafka1"));
        when(sniffer.getAssignment()).thenReturn(Set.of(PARTITION));
        when(stateChecker.checkLag()).thenReturn(new LagCheckResult(false, Set.of()));
        when(stateChecker.describe()).thenReturn("");
        when(kafkaProperties.getConnection(anyString())).thenReturn(new ConnectionProperties());
        when(kafkaProperties.getLagPollingIntervalMs()).thenReturn(1L);
        when(kafkaProperties.getReadinessPollingIntervalMs()).thenReturn(1L);
        when(kafkaProperties.getLagPollingTimeoutMs()).thenReturn(50L);
        when(kafkaProperties.getStartupStabilizationTimeoutSeconds()).thenReturn(3L);
        when(stateChecker.areExpectedGroupsReady(any())).thenReturn(true);
        when(stateChecker.areAllConsumerGroupsStable(any())).thenReturn(true);
        when(stateChecker.describeExpectedGroups(any())).thenReturn("g1=missing");
        when(stateChecker.describeGroupStates()).thenReturn("g1=missing");
    }

    @Test
    void startResetsWaitState() {
        connectionWithExpectedGroups("g1");

        waitCompletion.start();

        verify(sniffer).setLastException(null);
        verify(stateChecker).reset();
        verify(inboundMessageRegistry).reset();
    }

    @Test
    void startThrowsWhenExpectedGroupsNotReady() {
        connectionWithExpectedGroups("g1");
        when(kafkaProperties.getStartupStabilizationTimeoutSeconds()).thenReturn(0L);
        when(stateChecker.areExpectedGroupsReady(any())).thenReturn(false);

        assertThatThrownBy(() -> waitCompletion.start())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected consumer groups are not ready")
                .hasMessageContaining("g1=missing");
    }

    @Test
    void startProceedsWithoutExpectedGroupsWhenGroupsNotStable() {
        when(stateChecker.areAllConsumerGroupsStable(any())).thenReturn(false);
        when(kafkaProperties.getStartupStabilizationTimeoutSeconds()).thenReturn(0L);

        waitCompletion.start();

        verify(stateChecker).reset();
        verify(inboundMessageRegistry).reset();
    }

    @Test
    @SuppressWarnings("unchecked")
    void startAwaitsUnionOfExpectedGroupsFromAllConnections() {
        ConnectionProperties first = new ConnectionProperties();
        first.setExpectedGroups(Set.of("g1"));
        ConnectionProperties second = new ConnectionProperties();
        second.setExpectedGroups(Set.of("g2"));
        when(sniffer.getConnectionNames()).thenReturn(List.of("kafka1", "monitor"));
        when(kafkaProperties.getConnection("kafka1")).thenReturn(first);
        when(kafkaProperties.getConnection("monitor")).thenReturn(second);

        waitCompletion.start();

        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(stateChecker, atLeast(1)).areExpectedGroupsReady(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder("g1", "g2");
    }

    @Test
    void waitCompletionTimesOutWhenExpectedGroupsNotReady() {
        connectionWithExpectedGroups("g1");
        when(kafkaProperties.getLagPollingTimeoutMs()).thenReturn(30L);
        when(stateChecker.areExpectedGroupsReady(any())).thenReturn(false);
        when(stateChecker.checkLag()).thenReturn(new LagCheckResult(false, Set.of(PARTITION)));

        assertThatThrownBy(() -> waitCompletion.waitCompletion())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expected consumer groups are not ready")
                .hasMessageContaining("g1=missing");
    }

    @Test
    void waitCompletionTimesOutWhenSnifferNotAssigned() {
        when(kafkaProperties.getLagPollingTimeoutMs()).thenReturn(30L);
        when(sniffer.getAssignment()).thenReturn(Collections.emptySet());
        when(stateChecker.checkLag()).thenReturn(new LagCheckResult(false, Set.of(PARTITION)));

        assertThatThrownBy(() -> waitCompletion.waitCompletion())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("sniffer is not assigned to all partitions");
        verify(sniffer, atLeast(1)).signalMetadataNeedsRefresh();
    }

    @Test
    void waitCompletionTimesOutWhenLagPersists() {
        when(kafkaProperties.getLagPollingTimeoutMs()).thenReturn(30L);
        when(stateChecker.checkLag()).thenReturn(new LagCheckResult(true, Set.of(PARTITION)));

        assertThatThrownBy(() -> waitCompletion.waitCompletion())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Kafka wait completion timeout. Unprocessed messages remaining on broker.");
    }

    @Test
    void waitCompletionPropagatesSnifferError() {
        RuntimeException error = new RuntimeException("sniffer failed");
        when(sniffer.getLastException()).thenReturn(error);

        assertThatThrownBy(() -> waitCompletion.waitCompletion()).isSameAs(error);
    }

    @Test
    void waitCompletionWaitsUntilSutReady() {
        connectionWithExpectedGroups("g1");
        when(stateChecker.areExpectedGroupsReady(any())).thenReturn(false, false, true);
        when(stateChecker.checkLag()).thenReturn(new LagCheckResult(false, Set.of(PARTITION)));

        waitCompletion.waitCompletion();

        verify(stateChecker, atLeast(3)).areExpectedGroupsReady(any());
    }

    @Test
    void isAnyTaskStartedTrueAfterLagObserved() {
        when(stateChecker.checkLag())
                .thenReturn(new LagCheckResult(true, Set.of(PARTITION)), new LagCheckResult(false, Set.of(PARTITION)));

        waitCompletion.waitCompletion();

        assertThat(waitCompletion.isAnyTaskStarted()).isTrue();
    }

    @Test
    void isAnyTaskStartedFalseForCleanRun() {
        waitCompletion.waitCompletion();

        assertThat(waitCompletion.isAnyTaskStarted()).isFalse();
    }

    private void connectionWithExpectedGroups(String... groups) {
        ConnectionProperties connection = new ConnectionProperties();
        connection.setExpectedGroups(Set.of(groups));
        when(kafkaProperties.getConnection("kafka1")).thenReturn(connection);
    }
}

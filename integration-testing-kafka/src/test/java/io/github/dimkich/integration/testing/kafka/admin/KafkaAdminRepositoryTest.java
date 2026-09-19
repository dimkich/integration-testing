package io.github.dimkich.integration.testing.kafka.admin;

import io.github.dimkich.integration.testing.kafka.KafkaAdminRepository;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class KafkaAdminRepositoryTest {

    private AdminClient adminClient;
    private KafkaAdminRepository repository;

    @BeforeEach
    void setUp() {
        adminClient = mock(AdminClient.class);
        repository = new KafkaAdminRepository(adminClient);
    }

    @Test
    void allConsumerGroupsStable_noGroups() {
        stubGroups();

        assertThat(repository.areAllConsumerGroupsStable()).isTrue();
    }

    @Test
    void allConsumerGroupsStable_stableStates() {
        stubGroups(
                listing("g1", ConsumerGroupState.STABLE),
                listing("g2", ConsumerGroupState.EMPTY),
                listing("g3", ConsumerGroupState.DEAD));

        assertThat(repository.areAllConsumerGroupsStable()).isTrue();
    }

    @Test
    void allConsumerGroupsStable_rebalancingGroup() {
        stubGroups(
                listing("g1", ConsumerGroupState.STABLE),
                listing("g2", ConsumerGroupState.PREPARING_REBALANCE));

        assertThat(repository.areAllConsumerGroupsStable()).isFalse();
    }

    @Test
    void allConsumerGroupsStable_emptyListingWithExpectedGroup() {
        stubGroups();

        assertThat(repository.areAllConsumerGroupsStable(Set.of("expected-group"))).isFalse();
    }

    @Test
    void allConsumerGroupsStable_expectedGroupMissing() {
        stubGroups(listing("g1", ConsumerGroupState.STABLE));

        assertThat(repository.areAllConsumerGroupsStable(Set.of("g1", "expected-group"))).isFalse();
    }

    @Test
    void allConsumerGroupsStable_expectedGroupPresentAndStable() {
        stubGroups(listing("g1", ConsumerGroupState.STABLE));

        assertThat(repository.areAllConsumerGroupsStable(Set.of("g1"))).isTrue();
    }

    @Test
    void expectedGroupsReady_emptyExpectedDoesNotCallAdmin() {
        assertThat(repository.areExpectedGroupsReady(Set.of())).isTrue();

        verify(adminClient, never()).describeConsumerGroups(anyCollection());
    }

    @Test
    void expectedGroupsReady_stableAssignedMember() {
        stubDescriptions(Map.of("g1", completed(description(ConsumerGroupState.STABLE, member(true)))));

        assertThat(repository.areExpectedGroupsReady(Set.of("g1"))).isTrue();
    }

    @Test
    void expectedGroupsReady_missingGroup() {
        stubDescriptions(Map.of("g1", failed(new GroupIdNotFoundException("g1"))));

        assertThat(repository.areExpectedGroupsReady(Set.of("g1"))).isFalse();
    }

    @Test
    void expectedGroupsReady_notStable() {
        stubDescriptions(Map.of("g1", completed(description(ConsumerGroupState.PREPARING_REBALANCE, member(true)))));

        assertThat(repository.areExpectedGroupsReady(Set.of("g1"))).isFalse();
    }

    @Test
    void expectedGroupsReady_completingRebalance() {
        stubDescriptions(Map.of("g1", completed(description(ConsumerGroupState.COMPLETING_REBALANCE, member(true)))));

        assertThat(repository.areExpectedGroupsReady(Set.of("g1"))).isFalse();
    }

    @Test
    void expectedGroupsReady_noMembers() {
        stubDescriptions(Map.of("g1", completed(description(ConsumerGroupState.STABLE))));

        assertThat(repository.areExpectedGroupsReady(Set.of("g1"))).isFalse();
    }

    @Test
    void expectedGroupsReady_memberWithoutAssignment() {
        stubDescriptions(Map.of("g1", completed(description(ConsumerGroupState.STABLE, member(false)))));

        assertThat(repository.areExpectedGroupsReady(Set.of("g1"))).isFalse();
    }

    @Test
    void expectedGroupsReady_futureMissingFromResult() {
        stubDescriptions(Map.of());

        assertThat(repository.areExpectedGroupsReady(Set.of("g1"))).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void expectedGroupsReady_interrupted() throws Exception {
        KafkaFuture<ConsumerGroupDescription> future = mock(KafkaFuture.class);
        when(future.get()).thenThrow(new InterruptedException("interrupted"));
        stubDescriptions(Map.of("g1", future));

        assertThat(repository.areExpectedGroupsReady(Set.of("g1"))).isFalse();
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void describeExpectedGroups_empty() {
        assertThat(repository.describeExpectedGroups(Set.of())).isEqualTo("none");
    }

    @Test
    void describeExpectedGroups_readyAndMissing() {
        stubDescriptions(Map.of(
                "g1", completed(description(ConsumerGroupState.STABLE, member(true))),
                "g2", failed(new GroupIdNotFoundException("g2"))));
        Set<String> expected = new LinkedHashSet<>(List.of("g1", "g2"));

        assertThat(repository.describeExpectedGroups(expected)).isEqualTo("g1=ready, g2=missing");
    }

    @Test
    void describeExpectedGroups_notReady() {
        stubDescriptions(Map.of("g1", completed(description(ConsumerGroupState.PREPARING_REBALANCE, member(false)))));

        assertThat(repository.describeExpectedGroups(Set.of("g1")))
                .isEqualTo("g1=PreparingRebalance(members=1, assigned=0)");
    }

    private void stubGroups(ConsumerGroupListing... listings) {
        ListConsumerGroupsResult result = mock(ListConsumerGroupsResult.class);
        KafkaFutureImpl<Collection<ConsumerGroupListing>> future = new KafkaFutureImpl<>();
        future.complete(Arrays.asList(listings));
        when(result.all()).thenReturn(future);
        when(adminClient.listConsumerGroups()).thenReturn(result);
    }

    private void stubDescriptions(Map<String, KafkaFuture<ConsumerGroupDescription>> futures) {
        when(adminClient.describeConsumerGroups(anyCollection()))
                .thenReturn(new DescribeConsumerGroupsResult(futures));
    }

    private static <T> KafkaFuture<T> completed(T value) {
        KafkaFutureImpl<T> future = new KafkaFutureImpl<>();
        future.complete(value);
        return future;
    }

    private static KafkaFuture<ConsumerGroupDescription> failed(Throwable error) {
        KafkaFutureImpl<ConsumerGroupDescription> future = new KafkaFutureImpl<>();
        future.completeExceptionally(error);
        return future;
    }

    private static ConsumerGroupListing listing(String groupId, ConsumerGroupState state) {
        return new ConsumerGroupListing(groupId, false, Optional.of(state));
    }

    private static ConsumerGroupDescription description(ConsumerGroupState state, MemberDescription... members) {
        return new ConsumerGroupDescription("group", false, List.of(members), "range", state, new Node(1, "host", 9092));
    }

    private static MemberDescription member(boolean assigned) {
        Set<TopicPartition> partitions = assigned
                ? Set.of(new TopicPartition("topic", 0))
                : Set.of();
        return new MemberDescription("consumer-1", Optional.empty(), "client-1", "host",
                new MemberAssignment(partitions));
    }
}

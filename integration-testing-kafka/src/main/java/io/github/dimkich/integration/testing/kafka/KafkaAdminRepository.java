package io.github.dimkich.integration.testing.kafka;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * {@link KafkaStateChecker} backed by the Kafka Admin API: computes lag from the latest
 * end offsets and the committed offsets of all live consumer groups.
 *
 * <p>Also reports consumer-group readiness: whether all groups are stable
 * ({@link #areAllConsumerGroupsStable(Set)}) and whether the explicitly expected
 * groups of the system under test are running and assigned
 * ({@link #areExpectedGroupsReady(Set)}).
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaAdminRepository implements KafkaStateChecker {
    private final AdminClient adminClient;

    /**
     * @return {@code true} when every consumer group is in a state that
     *         indicates no in-flight rebalance, or when no groups exist at all.
     *         Returning {@code true} for the empty case prevents
     *         {@code KafkaWaitCompletion.start()} from blocking for the full
     *         stabilization timeout when there is nothing to stabilize.
     */
    public boolean areAllConsumerGroupsStable() {
        return areAllConsumerGroupsStable(Set.of());
    }

    /**
     * Returns whether all consumer groups are stable and every expected group is
     * present on the broker.
     *
     * <p>Unlike {@link #areAllConsumerGroupsStable()}, a missing group makes the
     * result {@code false} when {@code expectedGroups} is not empty. This keeps
     * {@code KafkaWaitCompletion.start()} waiting for SUT consumer groups that
     * appear asynchronously — an empty broker listing is no longer treated as
     * "nothing to stabilize" when groups are explicitly awaited.
     *
     * @param expectedGroups consumer groups that must exist on the broker
     * @return {@code true} when every existing group is stable and all expected
     *         groups are present
     */
    @Override
    @SneakyThrows
    public boolean areAllConsumerGroupsStable(Set<String> expectedGroups) {
        List<ConsumerGroupListing> groups = adminClient.listConsumerGroups().all().get().stream().toList();
        if (!expectedGroups.isEmpty()) {
            Set<String> present = groups.stream()
                    .map(ConsumerGroupListing::groupId)
                    .collect(Collectors.toSet());
            if (!present.containsAll(expectedGroups)) {
                return false;
            }
        }
        if (groups.isEmpty()) {
            return true;
        }
        return groups.stream().allMatch(KafkaAdminRepository::isStable);
    }

    private static boolean isStable(ConsumerGroupListing group) {
        ConsumerGroupState s = group.state().orElse(null);
        return s == ConsumerGroupState.STABLE
                || s == ConsumerGroupState.EMPTY
                || s == ConsumerGroupState.DEAD;
    }

    /**
     * Returns whether all expected consumer groups are ready to consume: each
     * group exists, is {@link ConsumerGroupState#STABLE}, and its members have
     * non-empty partition assignments ("running + assigned").
     *
     * <p>An empty set is always ready — the no-op mode for tests without
     * configured {@code expected-groups}. A group that is missing, still
     * rebalancing or has members without assignments makes the result
     * {@code false}, so {@code KafkaWaitCompletion} keeps waiting instead of
     * declaring the test complete.
     *
     * @param expectedGroups consumer groups to check
     * @return {@code true} when every expected group is ready
     */
    @Override
    public boolean areExpectedGroupsReady(Set<String> expectedGroups) {
        if (expectedGroups.isEmpty()) {
            return true;
        }
        Map<String, KafkaFuture<ConsumerGroupDescription>> futures =
                adminClient.describeConsumerGroups(expectedGroups).describedGroups();
        for (String groupId : expectedGroups) {
            KafkaFuture<ConsumerGroupDescription> future = futures.get(groupId);
            if (future == null) {
                return false;
            }
            ConsumerGroupDescription description;
            try {
                description = future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (ExecutionException e) {
                log.debug("Consumer group [{}] is not ready: {}", groupId, String.valueOf(e.getCause()));
                return false;
            }
            if (!isReady(description)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isReady(ConsumerGroupDescription description) {
        if (description.state() != ConsumerGroupState.STABLE) {
            return false;
        }
        if (description.members().isEmpty()) {
            return false;
        }
        return description.members().stream()
                .noneMatch(member -> member.assignment().topicPartitions().isEmpty());
    }

    /**
     * Returns a comma-separated readiness report of the expected groups, used in
     * logs and timeout messages.
     *
     * @param expectedGroups consumer groups to describe
     * @return entries in the form {@code groupId=ready},
     *         {@code groupId=STATE(members=N, assigned=M)} or
     *         {@code groupId=missing}; {@code none} for an empty set
     */
    @Override
    public String describeExpectedGroups(Set<String> expectedGroups) {
        if (expectedGroups.isEmpty()) {
            return "none";
        }
        Map<String, KafkaFuture<ConsumerGroupDescription>> futures =
                adminClient.describeConsumerGroups(expectedGroups).describedGroups();
        return expectedGroups.stream()
                .map(groupId -> groupId + "=" + describeGroup(futures.get(groupId)))
                .collect(Collectors.joining(", "));
    }

    private static String describeGroup(KafkaFuture<ConsumerGroupDescription> future) {
        if (future == null) {
            return "missing";
        }
        ConsumerGroupDescription description;
        try {
            description = future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "interrupted";
        } catch (ExecutionException e) {
            return "missing";
        }
        if (isReady(description)) {
            return "ready";
        }
        long assigned = description.members().stream()
                .filter(member -> !member.assignment().topicPartitions().isEmpty())
                .count();
        return description.state() + "(members=" + description.members().size()
                + ", assigned=" + assigned + ")";
    }

    /**
     * Returns a comma-separated list of consumer group states, used in log messages.
     *
     * @return group states in the form {@code groupId=STATE}
     */
    @Override
    @SneakyThrows
    public String describeGroupStates() {
        return adminClient.listConsumerGroups().all().get().stream()
                .map(g -> g.groupId() + "=" + g.state().orElse(null))
                .collect(Collectors.joining(", "));
    }

    /**
     * Computes lag across all topics.
     *
     * <p>Only <b>live</b> consumer groups contribute committed offsets: groups
     * in {@link ConsumerGroupState#EMPTY} or {@link ConsumerGroupState#DEAD}
     * state have no active members, and their committed offsets are frozen
     * artifacts. A single such group with committed offset {@code 0} for a
     * partition used to pin the aggregated value to {@code 0} via
     * {@code Math::min} and report perpetual lag even when every live group had
     * caught up.
     *
     * <p>For every remaining partition the aggregated committed offset is the
     * minimum across all live groups that have committed for it. Partitions for
     * which no live group has committed yet are treated as
     * {@code committed = 0}: this preserves the "wait until every live group has
     * caught up" semantics, which is what keeps {@code KafkaWaitCompletion}
     * blocked until sniffer and SUT have both consumed the message. Without this
     * default, a freshly auto-created topic with a single message would be seen
     * as having no lag the moment the framework starts waiting — sniffer would
     * not have time to poll, and the outbound message would be missed.
     *
     * <p>The returned partition set is the complete set of (non-internal)
     * topic partitions. {@code KafkaWaitCompletion} uses it to verify that the
     * sniffer sees all partitions, independent of which groups consume them.
     */
    @SneakyThrows
    public LagCheckResult checkLag() {
        Set<String> topics = adminClient.listTopics().names().get().stream()
                .filter(t -> !t.startsWith("__"))
                .collect(Collectors.toSet());

        Map<TopicPartition, OffsetSpec> endOffsetsReq = adminClient.describeTopics(topics).allTopicNames().get()
                .values().stream()
                .flatMap(desc -> desc.partitions().stream()
                        .map(p -> new TopicPartition(desc.name(), p.partition())))
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));

        if (endOffsetsReq.isEmpty()) {
            return new LagCheckResult(false, Set.of());
        }

        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                adminClient.listOffsets(endOffsetsReq).all().get();

        List<ConsumerGroupListing> liveGroups = adminClient.listConsumerGroups().all().get().stream()
                .filter(g -> {
                    ConsumerGroupState s = g.state().orElse(null);
                    return s != ConsumerGroupState.EMPTY && s != ConsumerGroupState.DEAD;
                })
                .toList();

        if (liveGroups.isEmpty()) {
            return new LagCheckResult(false, endOffsetsReq.keySet());
        }

        Map<String, ListConsumerGroupOffsetsSpec> groupSpecs = liveGroups.stream()
                .collect(Collectors.toMap(ConsumerGroupListing::groupId,
                        g -> new ListConsumerGroupOffsetsSpec()));

        Map<TopicPartition, Long> committed = adminClient.listConsumerGroupOffsets(groupSpecs).all().get()
                .values().stream()
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().offset(),
                        Math::min));

        boolean hasLag = false;
        for (TopicPartition tp : endOffsetsReq.keySet()) {
            long endOffset = endOffsets.get(tp).offset();
            long committedOffset = committed.getOrDefault(tp, 0L);
            if (endOffset > committedOffset) {
                log.trace("Lag detected on [{}]: endOffset={}, committed={}", tp, endOffset, committedOffset);
                hasLag = true;
            }
        }

        return new LagCheckResult(hasLag, endOffsetsReq.keySet());
    }

    @Override
    public String describe() {
        return "groups: [" + describeGroupStates() + "]";
    }
}

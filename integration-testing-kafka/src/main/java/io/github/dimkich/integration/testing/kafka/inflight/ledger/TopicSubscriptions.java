package io.github.dimkich.integration.testing.kafka.inflight.ledger;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Tracks which topics each consumer group is subscribed to.
 *
 * <p>Two subscription shapes are supported:
 * <ul>
 *   <li>{@code registerExact} — the group subscribed to a fixed set of topics
 *       ({@code KafkaConsumer.subscribe(Collection)} or {@code assign(Collection)});</li>
 *   <li>{@code registerPattern} — the group subscribed via a regex
 *       ({@code KafkaConsumer.subscribe(Pattern)}).</li>
 * </ul>
 *
 * <p>Pattern matching results are memoized per {@code groupId#topic} to keep the
 * per-message hot path a plain {@link ConcurrentHashMap} lookup. The cache is
 * cleared on {@link #clearPatternCache()} (test boundary) and pruned on
 * {@link #removeGroup(String)} (dead group), so it never outlives its group.
 *
 * <p>Package-private: detail of {@link ClusterState}, not part of the ledger's
 * public surface.
 */
@Slf4j
final class TopicSubscriptions {

    private final ConcurrentHashMap<String, Set<String>> exact = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Pattern> patterns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> patternCache = new ConcurrentHashMap<>();

    void registerExact(String groupId, Collection<String> topics) {
        exact.put(groupId, new HashSet<>(topics));
        log.debug("registered exact subscription: group [{}] -> topics {}", groupId, topics);
    }

    void registerPattern(String groupId, Pattern pattern) {
        patterns.put(groupId, pattern);
        log.debug("registered pattern subscription: group [{}] -> pattern {}", groupId, pattern);
    }

    /** {@code true} if {@code groupId} subscribes to {@code topic}, exact or by pattern. */
    boolean isSubscribed(String groupId, String topic) {
        Set<String> e = exact.get(groupId);
        if (e != null && e.contains(topic)) {
            return true;
        }
        Pattern p = patterns.get(groupId);
        if (p == null) {
            return false;
        }
        String cacheKey = groupId + "#" + topic;
        Boolean cached = patternCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        boolean matched = p.matcher(topic).matches();
        patternCache.put(cacheKey, matched);
        return matched;
    }

    /** All group ids that currently have at least one subscription. */
    Set<String> groupIds() {
        Set<String> all = new HashSet<>(exact.keySet());
        all.addAll(patterns.keySet());
        return all;
    }

    /** {@code true} if the group has at least one registered subscription. */
    boolean hasGroup(String groupId) {
        return exact.containsKey(groupId) || patterns.containsKey(groupId);
    }

    /** Drops all subscription state for a dead group. */
    void removeGroup(String groupId) {
        exact.remove(groupId);
        patterns.remove(groupId);
        patternCache.keySet().removeIf(k -> k.startsWith(groupId + "#"));
    }

    /** Clears only the pattern-match cache; subscriptions themselves are preserved. */
    void clearPatternCache() {
        patternCache.clear();
    }
}
package io.github.dimkich.integration.testing.kafka.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Resolves {@link KafkaTopicMetadata} for topic names against a set of configured regex patterns.
 * <p>
 * Patterns are matched against the topic name in registration order and the <b>first</b> matching
 * pattern wins; otherwise the connection {@code defaultMetadata} is returned. Registration order
 * mirrors the iteration order of the {@code topics} map (see
 * {@link io.github.dimkich.integration.testing.kafka.config.ConnectionProperties#getTopics()}),
 * which Spring binds as a {@link java.util.LinkedHashMap}, so patterns are evaluated in the order
 * they appear in the configuration file.
 * <p>
 * Do not rely on a specific evaluation order when several overlapping patterns can match the same
 * topic: if the underlying map is built as a {@link java.util.HashMap} (or any other map without a
 * defined order), the winning pattern (and therefore the resolved metadata) becomes
 * non-deterministic. To avoid ambiguity, keep patterns non-overlapping. The first resolution per
 * topic name is cached, so a non-deterministic outcome may also persist for the lifetime of this
 * registry.
 */
@Slf4j
@RequiredArgsConstructor
public class ConnectionTopicRegistry {
    private final KafkaTopicMetadata defaultMetadata;
    private final Map<Pattern, KafkaTopicMetadata> configuredTopics = new LinkedHashMap<>();
    private final Map<String, KafkaTopicMetadata> resolvedCache = new ConcurrentHashMap<>();

    /**
     * Registers a regex topic pattern associated with {@code metadata}. Patterns are matched in
     * registration order and the first matching pattern wins (see class javadoc).
     */
    public void registerTopicPattern(String pattern, KafkaTopicMetadata metadata) {
        configuredTopics.put(Pattern.compile(pattern), metadata);
    }

    /**
     * Returns metadata for {@code topicName}. The resolution is performed once per topic name and
     * cached, so subsequent calls return the same {@link KafkaTopicMetadata} instance.
     */
    public KafkaTopicMetadata getMetadata(String topicName) {
        return resolvedCache.computeIfAbsent(topicName, this::resolveMetadata);
    }

    private KafkaTopicMetadata resolveMetadata(String topicName) {
        for (Map.Entry<Pattern, KafkaTopicMetadata> entry : configuredTopics.entrySet()) {
            if (entry.getKey().matcher(topicName).matches()) {
                log.trace("Topic [{}] matched pattern [{}]", topicName, entry.getKey());
                return entry.getValue();
            }
        }

        log.trace("Topic [{}] did not match any pattern, falling back to connection defaults", topicName);
        return defaultMetadata;
    }
}

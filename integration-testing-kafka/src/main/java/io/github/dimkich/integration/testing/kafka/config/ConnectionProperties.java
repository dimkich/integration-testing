package io.github.dimkich.integration.testing.kafka.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration of a single Kafka connection: bootstrap servers, client properties
 * and per-topic overrides. Inherits top-level defaults from {@link TopicProperties}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConnectionProperties extends TopicProperties {
    private String bootstrapServers;
    private Map<String, TopicProperties> topics;
    private Map<String, Object> properties = Map.of();

    /**
     * Consumer groups of the system under test that must be ready before a test
     * may proceed. Each group is awaited by
     * {@link io.github.dimkich.integration.testing.kafka.KafkaWaitCompletion}:
     * the group must exist, be {@code STABLE} and have live members with assigned
     * partitions. An empty set disables the waiting (pre-existing behavior).
     */
    private Set<String> expectedGroups;

    /**
     * Builds the client configuration map for this connection: the configured client
     * properties plus the bootstrap servers.
     *
     * @return a new mutable map of Kafka client properties
     */
    public Map<String, Object> toClientProperties() {
        Map<String, Object> config = new HashMap<>(properties);
        config.put("bootstrap.servers", bootstrapServers);
        return config;
    }
}

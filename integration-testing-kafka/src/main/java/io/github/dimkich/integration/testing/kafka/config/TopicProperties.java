package io.github.dimkich.integration.testing.kafka.config;

import lombok.Data;

import java.util.Set;

/**
 * Per-connection and per-topic Kafka settings: serde configuration, ignore flags and
 * fields excluded from comparison. Values are inherited from the connection level to
 * the topic level.
 */
@Data
public class TopicProperties {
    private RecordProperties serializer;
    private RecordProperties deserializer;
    private boolean ignore;
    private boolean ignoreInbound = true;
    private Set<String> excludedFields;
}

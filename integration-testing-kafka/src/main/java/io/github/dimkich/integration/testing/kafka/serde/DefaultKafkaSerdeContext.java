package io.github.dimkich.integration.testing.kafka.serde;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.header.Headers;

/**
 * Default immutable {@link KafkaSerdeContext} carrying the topic and native headers of
 * the record being processed.
 */
@Getter
@RequiredArgsConstructor
public class DefaultKafkaSerdeContext implements KafkaSerdeContext {
    private final String topic;
    private final Headers headers;
}

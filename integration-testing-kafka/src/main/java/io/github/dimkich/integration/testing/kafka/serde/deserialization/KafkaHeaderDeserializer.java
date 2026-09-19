package io.github.dimkich.integration.testing.kafka.serde.deserialization;

import org.apache.kafka.common.header.Headers;
import org.springframework.util.MultiValueMap;

/**
 * Deserializes native Kafka {@link Headers} into a message header map.
 */
@FunctionalInterface
public interface KafkaHeaderDeserializer {
    /**
     * Copies the native headers into the target map.
     *
     * @param source the native Kafka headers
     * @param target the message header map to fill
     */
    void deserialize(Headers source, MultiValueMap<String, Object> target);
}

package io.github.dimkich.integration.testing.kafka.serde.serialization;

import org.apache.kafka.common.header.Headers;
import org.springframework.util.MultiValueMap;

/**
 * Serializes a message header map into native Kafka {@link Headers}.
 */
@FunctionalInterface
public interface KafkaHeaderSerializer {
    /**
     * Copies the source headers into the target native headers.
     *
     * @param source the message headers
     * @param target the native Kafka headers to fill
     */
    void serialize(MultiValueMap<String, Object> source, Headers target);
}

package io.github.dimkich.integration.testing.kafka.serde;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import org.apache.kafka.common.header.Headers;

/**
 * Typed Kafka context exposing the topic and native Kafka {@link Headers}.
 */
public interface KafkaSerdeContext extends TestSerdeContext {

    /**
     * Returns the topic of the record being serialized or deserialized.
     *
     * @return the topic name
     */
    String getTopic();

    /**
     * Returns the native Kafka headers of the record being serialized or deserialized.
     *
     * @return the record headers
     */
    Headers getHeaders();
}

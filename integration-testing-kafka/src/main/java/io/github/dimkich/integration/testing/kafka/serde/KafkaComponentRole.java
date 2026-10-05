package io.github.dimkich.integration.testing.kafka.serde;

import io.github.dimkich.integration.testing.serde.ComponentRole;

/**
 * Kafka record parts that can be configured independently: key, value and headers.
 *
 * <p>Used as a {@link ComponentRole} to restrict a component to a single part of a record.
 */
public enum KafkaComponentRole implements ComponentRole {
    KEY,
    VALUE,
    HEADERS
}

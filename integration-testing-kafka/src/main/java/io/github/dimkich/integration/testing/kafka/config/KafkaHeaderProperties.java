package io.github.dimkich.integration.testing.kafka.config;

import io.github.dimkich.integration.testing.serde.SerdeRoleCollector;
import io.github.dimkich.integration.testing.serde.properties.BeanRefProperties;
import io.github.dimkich.integration.testing.serde.properties.ClassRefProperties;
import io.github.dimkich.integration.testing.serde.properties.ProviderProperties;

/**
 * Serde contract of the Kafka {@code headers} component: the core source interfaces
 * ({@link ProviderProperties}, {@link BeanRefProperties}, {@link ClassRefProperties})
 * without {@code BinaryEnvelopeProperties}. Binary envelopes are a key/value feature and
 * are not part of the header contract.
 */
public interface KafkaHeaderProperties extends ProviderProperties, BeanRefProperties, ClassRefProperties {

    @Override
    default void reportRoles(SerdeRoleCollector collector) {
        BeanRefProperties.super.reportRoles(collector);
        ProviderProperties.super.reportRoles(collector);
        ClassRefProperties.super.reportRoles(collector);
    }
}

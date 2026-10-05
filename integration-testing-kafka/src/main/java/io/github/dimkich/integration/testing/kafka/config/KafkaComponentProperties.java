package io.github.dimkich.integration.testing.kafka.config;

import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Serde configuration of a single Kafka record component ({@code key} or {@code value}): the
 * core serde properties plus Spring Kafka type-info settings.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KafkaComponentProperties extends StandardSerdeProperties {
    private Boolean addTypeInfoHeaders;
    private Boolean useTypeInfoHeaders;
    private String trustedPackages;
}

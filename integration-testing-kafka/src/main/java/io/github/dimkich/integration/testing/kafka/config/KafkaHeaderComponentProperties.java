package io.github.dimkich.integration.testing.kafka.config;

import io.github.dimkich.integration.testing.config.PropertyInheritanceExclusive;
import lombok.Data;

import java.lang.reflect.Type;

/**
 * Serde configuration of the Kafka {@code headers} component: a projection of the core
 * source interfaces ({@link KafkaHeaderProperties}) without the binary envelope. Headers
 * are serialized per value by a context-free converter; {@code binary-envelope} is not part
 * of the contract and a configured one is rejected at binding time.
 */
@Data
public class KafkaHeaderComponentProperties implements KafkaHeaderProperties {
    @PropertyInheritanceExclusive("ref")
    private String beanRef;

    @PropertyInheritanceExclusive("ref")
    private String type;

    private Type targetClass;
    private String objectMapperRef;

    private Boolean addTypeInfoHeaders;
    private Boolean useTypeInfoHeaders;
    private String trustedPackages;
}

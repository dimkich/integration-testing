package io.github.dimkich.integration.testing.serde;

import io.github.dimkich.integration.testing.config.PropertyInheritanceExclusive;
import io.github.dimkich.integration.testing.serde.properties.BeanRefProperties;
import io.github.dimkich.integration.testing.serde.properties.BinaryEnvelopeProperties;
import io.github.dimkich.integration.testing.serde.properties.ClassRefProperties;
import io.github.dimkich.integration.testing.serde.properties.ProviderProperties;
import lombok.Data;

import java.lang.reflect.Type;

/**
 * Standard serde configuration shared by all modules: the mutually exclusive {@code type} and
 * {@code beanRef} sources, an optional {@code targetClass} and {@code objectMapperRef} applied by
 * providers, and an optional {@code binaryEnvelope} wrapping the resolved converter.
 *
 * <p>Sources are exclusive at the same configuration level
 * ({@link PropertyInheritanceExclusive}), so a module-specific property (e.g. a Kafka record
 * component) can override an inherited source.
 */
@Data
public class StandardSerdeProperties implements ProviderProperties, BeanRefProperties, ClassRefProperties,
        BinaryEnvelopeProperties {
    @PropertyInheritanceExclusive("ref")
    private String beanRef;

    @PropertyInheritanceExclusive("ref")
    private String type;

    private Type targetClass;
    private String objectMapperRef;
    private String binaryEnvelope;

    @Override
    public void reportRoles(SerdeRoleCollector collector) {
        BeanRefProperties.super.reportRoles(collector);
        ProviderProperties.super.reportRoles(collector);
        ClassRefProperties.super.reportRoles(collector);
        BinaryEnvelopeProperties.super.reportRoles(collector);
    }
}

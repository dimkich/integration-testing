package io.github.dimkich.integration.testing.serde.properties;

import io.github.dimkich.integration.testing.serde.SerdeRoleCollector;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;

/**
 * Configuration with a named provider source and its optional customizations.
 */
public interface ProviderProperties extends TypeProperties {

    /**
     * Returns the type the provider should deserialize to.
     *
     * @return the target type, may be {@code null}
     */
    Type getTargetClass();

    /**
     * Returns the bean name of the mapper (e.g. {@code ObjectMapper}) used by the provider.
     *
     * @return the mapper bean name, may be {@code null}
     */
    String getObjectMapperRef();

    @Override
    default void reportRoles(SerdeRoleCollector collector) {
        if (hasType() && !getType().contains(".")) {
            collector.addParametricSource("type", "points to a provider or a fully qualified class name");
        }
        if (getTargetClass() != null || StringUtils.hasText(getObjectMapperRef())) {
            collector.addOption("targetClass'/'objectMapperRef");
        }
    }
}

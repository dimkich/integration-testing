package io.github.dimkich.integration.testing.serde.properties;

import io.github.dimkich.integration.testing.serde.SerdeRoleCollector;

/**
 * Configuration whose {@code type} is a fully qualified class name instantiated by Spring.
 *
 * @see ClassRefConverterFactory
 */
public interface ClassRefProperties extends TypeProperties {

    @Override
    default void reportRoles(SerdeRoleCollector collector) {
        if (hasType() && getType().contains(".")) {
            collector.addNonParametricSource("type", "points to a provider or a fully qualified class name",
                    "is a fully qualified class name (contains a dot)");
        }
    }
}

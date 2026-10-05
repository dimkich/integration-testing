package io.github.dimkich.integration.testing.serde.properties;

import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration with a named source: a provider name (e.g. {@code json}) or a fully qualified
 * class name.
 */
public interface TypeProperties extends TestSerdeProperties {

    /**
     * Returns the configured source name.
     *
     * @return the provider name or class name, may be {@code null}
     */
    String getType();

    /**
     * Checks whether {@code type} is set.
     *
     * @return {@code true} when the type is not blank
     */
    default boolean hasType() {
        return StringUtils.hasText(getType());
    }
}

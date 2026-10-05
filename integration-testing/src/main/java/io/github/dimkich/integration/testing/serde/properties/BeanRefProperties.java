package io.github.dimkich.integration.testing.serde.properties;

import io.github.dimkich.integration.testing.serde.SerdeRoleCollector;
import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration with a Spring bean reference as the serde source.
 *
 * <p>{@code beanRef} returns an existing bean as is, so provider options are not applied to it
 * (reported through {@link #reportRoles(SerdeRoleCollector)} and rejected by
 * {@link SerdeRoleCollector#validate()}).
 *
 * @see BeanRefConverterFactory
 */
public interface BeanRefProperties extends TestSerdeProperties {

    /**
     * Returns the name of the bean used as the serde source.
     *
     * @return the bean name, may be {@code null}
     */
    String getBeanRef();

    /**
     * Checks whether {@code beanRef} is set.
     *
     * @return {@code true} when the bean reference is not blank
     */
    default boolean hasBeanRef() {
        return StringUtils.hasText(getBeanRef());
    }

    @Override
    default void reportRoles(SerdeRoleCollector collector) {
        if (hasBeanRef()) {
            collector.addNonParametricSource("beanRef", "points to an existing bean", "is set");
        }
    }
}

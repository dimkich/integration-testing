package io.github.dimkich.integration.testing.serde;

/**
 * Marker contract for module serde configuration classes.
 *
 * <p>A configuration reports its roles (sources, options, decorators) to the pipeline through
 * {@link #reportRoles(SerdeRoleCollector)}; a class combining several role interfaces must
 * list them explicitly and call the corresponding {@code super} methods. The pipeline validates
 * the collected roles by {@link SerdeRoleCollector#validate()}. Implementations report invalid
 * combinations by throwing {@link IllegalArgumentException}.
 */
public interface TestSerdeProperties {

    /**
     * Reports the roles of this configuration level to the given collector. The default
     * implementation reports nothing.
     *
     * @param collector the collector to report to
     */
    default void reportRoles(SerdeRoleCollector collector) {
    }
}

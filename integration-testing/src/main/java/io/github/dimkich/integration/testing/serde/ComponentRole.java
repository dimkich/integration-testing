package io.github.dimkich.integration.testing.serde;

/**
 * Role of a serde component within a record, e.g. value, key or headers.
 *
 * <p>The core defines no fixed roles: every module declares its own enum
 * (e.g. {@code KafkaComponentRole}, {@code RedisComponentRole}) and roles are matched by
 * {@link #name()}. A role may also carry request data; in that case a component can accept the
 * whole role class through {@link TestSerdeComponent#getRoleClass()} instead of matching one
 * {@code name()}. A component without a role is universal and participates in any request.
 */
public interface ComponentRole {

    /**
     * Returns the role name.
     *
     * @return the name used to match a requested role
     */
    String name();
}

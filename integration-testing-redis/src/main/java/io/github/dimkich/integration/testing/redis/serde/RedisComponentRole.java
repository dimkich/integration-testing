package io.github.dimkich.integration.testing.redis.serde;

import io.github.dimkich.integration.testing.serde.ComponentRole;

/**
 * Redis data parts that can be configured independently: a plain value, a hash key and a hash
 * value.
 *
 * <p>Used as a {@link ComponentRole} to restrict a component to a single part of a Redis entry.
 */
public enum RedisComponentRole implements ComponentRole {
    VALUE,
    HASH_KEY,
    HASH_VALUE
}

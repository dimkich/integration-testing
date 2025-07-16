package io.github.dimkich.integration.testing.redis.redisson.convert;

import java.util.Set;

public interface RBridge {
    void set(Object value);

    Object get();

    void clear();

    void excludeFields(Set<String> fields);
}

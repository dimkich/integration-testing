package io.github.dimkich.integration.testing.redis.redisson.convert;

public interface RBridge {
    void set(Object value);

    Object get();

    void clear();
}

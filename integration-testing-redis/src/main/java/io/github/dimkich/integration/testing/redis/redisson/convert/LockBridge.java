package io.github.dimkich.integration.testing.redis.redisson.convert;

import java.util.Set;

public class LockBridge implements RBridge {

    @Override
    public void set(Object value) {
    }

    @Override
    public Object get() {
        return null;
    }

    @Override
    public void clear() {
    }

    @Override
    public void excludeFields(Set<String> fields) {
    }
}

package io.github.dimkich.integration.testing.redis.redisson.convert;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;

@RequiredArgsConstructor
public class CollectionBridge implements RBridge {
    private final Collection<Object> collection;

    @Override
    public void set(Object value) {
        collection.addAll((Collection<?>) value);
    }

    @Override
    public Object get() {
        return new ArrayList<>(collection);
    }

    @Override
    public void clear() {
        collection.clear();
    }
}

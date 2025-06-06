package io.github.dimkich.integration.testing;

import java.util.Map;

public interface TestDataStorage {
    String getName();

    Map<Object, Object> getCurrentValue();

    boolean isEmpty();

    void clear();
}

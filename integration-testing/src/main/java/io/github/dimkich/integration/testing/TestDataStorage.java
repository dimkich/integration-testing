package io.github.dimkich.integration.testing;

import java.util.Map;

public interface TestDataStorage {
    String getName();

    Map<String, Object> getCurrentValue() throws Exception;
}

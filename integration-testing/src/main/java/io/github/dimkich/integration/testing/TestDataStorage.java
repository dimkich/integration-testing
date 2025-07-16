package io.github.dimkich.integration.testing;

import java.util.Map;
import java.util.Set;

public interface TestDataStorage {
    String getName();

    Map<String, Object> getCurrentValue(Map<String, Set<String>> excludedFields) throws Exception;
}

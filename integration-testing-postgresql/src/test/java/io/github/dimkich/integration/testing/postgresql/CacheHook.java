package io.github.dimkich.integration.testing.postgresql;

import java.util.ArrayList;
import java.util.List;

/**
 * Test bean used as a {@code onTableChange} to observe when the application cache is refreshed. Every
 * hook method records its table name; {@link #drain()} returns (and clears) the recorded calls so a
 * test can assert how many hooks were invoked.
 */
public class CacheHook {
    private final List<String> calls = new ArrayList<>();

    public void reloadUsers() {
        calls.add("users");
    }

    public void reloadOrders() {
        calls.add("orders");
    }

    public List<String> drain() {
        List<String> result = new ArrayList<>(calls);
        calls.clear();
        return result;
    }
}

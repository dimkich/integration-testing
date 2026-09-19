package io.github.dimkich.integration.testing.execution.hook;

import java.util.ArrayList;
import java.util.List;

public class HookEvents {
    private static final List<String> events = new ArrayList<>();

    public static void reset() {
        events.clear();
    }

    public static void add(String event) {
        events.add(event);
    }

    public static List<String> get() {
        return new ArrayList<>(events);
    }
}

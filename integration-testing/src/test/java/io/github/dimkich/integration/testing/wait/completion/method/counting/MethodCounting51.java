package io.github.dimkich.integration.testing.wait.completion.method.counting;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class MethodCounting51 {
    private static void method() {
        MethodCalls.add(MethodCounting51.class, "method (count = "
                + MethodCountingTracker.getActiveTasks() + ")");
    }
}

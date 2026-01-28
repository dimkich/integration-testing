package io.github.dimkich.integration.testing.wait.completion.method.pair;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class MethodPair22 {
    public static void method() {
        MethodCalls.add(MethodPair22.class, "method (count = " + MethodPairTracker.getActiveTasks() + ")");
    }
}

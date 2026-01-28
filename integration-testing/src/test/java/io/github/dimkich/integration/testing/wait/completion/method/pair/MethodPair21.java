package io.github.dimkich.integration.testing.wait.completion.method.pair;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class MethodPair21 {
    public static void method() {
        MethodCalls.add(MethodPair21.class, "method (count = " + MethodPairTracker.getActiveTasks() + ")");
    }
}

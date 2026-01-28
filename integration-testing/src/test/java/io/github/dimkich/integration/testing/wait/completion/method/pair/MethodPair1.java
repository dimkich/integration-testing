package io.github.dimkich.integration.testing.wait.completion.method.pair;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class MethodPair1 {
    public void method() {
        MethodCalls.add(getClass(), "method (count = " + MethodPairTracker.getActiveTasks() + ")");
    }
}

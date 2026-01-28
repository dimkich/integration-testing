package io.github.dimkich.integration.testing.wait.completion.method.counting;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class MethodCounting43 {
    public void method() {
        MethodCalls.add(getClass(), "method (count = " + MethodCountingTracker.getActiveTasks() + ")");
    }
}

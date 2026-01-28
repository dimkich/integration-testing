package io.github.dimkich.integration.testing.wait.completion.method.counting;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class MethodCounting61<T> {
    public void method() {
        new Processor().method();
    }

    private class Processor {
        private void method() {
            MethodCalls.add(getClass(), "method (count = " + MethodCountingTracker.getActiveTasks() + ")");
        }
    }
}

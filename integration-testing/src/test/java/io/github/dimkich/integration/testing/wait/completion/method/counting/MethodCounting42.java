package io.github.dimkich.integration.testing.wait.completion.method.counting;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class MethodCounting42 {
    private MethodCounting41 methodCounting41 = new MethodCounting41();

    @MethodCounting4Ann
    public void method() {
        methodCounting41.method();
        MethodCalls.add(getClass(), "method (count = " + MethodCountingTracker.getActiveTasks() + ")");
    }
}

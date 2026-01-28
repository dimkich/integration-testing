package io.github.dimkich.integration.testing.wait.completion.future.like;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class FutureLike3 {
    @FutureLike3Ann
    public FutureLike3 create() {
        return new FutureLike3();
    }

    public void await() {
        MethodCalls.add(getClass(), "await");
    }
}

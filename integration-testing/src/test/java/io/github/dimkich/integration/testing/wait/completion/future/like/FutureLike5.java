package io.github.dimkich.integration.testing.wait.completion.future.like;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class FutureLike5 {
    public FutureLike5 create() {
        return new FutureLike5();
    }

    public void await() {
        MethodCalls.add(getClass(), "await");
    }
}

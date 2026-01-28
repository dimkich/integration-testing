package io.github.dimkich.integration.testing.wait.completion.future.like;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class FutureLike6 {
    static FutureLike6 create() {
        return new FutureLike6();
    }

    public void await() {
        MethodCalls.add(getClass(), "await");
    }
}

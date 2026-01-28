package io.github.dimkich.integration.testing.wait.completion.future.like;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class FutureLike2 {
    public void await() {
        MethodCalls.add(getClass(), "await");
    }
}

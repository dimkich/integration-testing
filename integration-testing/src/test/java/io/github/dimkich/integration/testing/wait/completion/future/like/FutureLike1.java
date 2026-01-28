package io.github.dimkich.integration.testing.wait.completion.future.like;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class FutureLike1 {
    public void await() {
        MethodCalls.add(getClass(), "await");
    }
}

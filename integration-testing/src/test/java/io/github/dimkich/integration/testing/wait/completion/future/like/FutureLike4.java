package io.github.dimkich.integration.testing.wait.completion.future.like;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

@FutureLike4Ann
public class FutureLike4 {
    public void await() {
        MethodCalls.add(getClass(), "await");
    }
}

package io.github.dimkich.integration.testing.wait.completion.future.like;

public class FutureLike3Child1 extends FutureLike3 {
    public FutureLike3Child2 create() {
        return new FutureLike3Child2();
    }
}

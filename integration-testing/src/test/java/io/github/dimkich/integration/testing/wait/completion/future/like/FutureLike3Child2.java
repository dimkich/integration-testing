package io.github.dimkich.integration.testing.wait.completion.future.like;

public class FutureLike3Child2 extends FutureLike3 {
    @FutureLike3Ann
    public FutureLike3Child2 create() {
        return new FutureLike3Child2();
    }

    @Override
    public void await() {
        super.await();
    }
}

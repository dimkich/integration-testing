package io.github.dimkich.integration.testing.wait.completion.future.like;

import java.util.function.Consumer;

public class FutureLike5Consumer implements Consumer<Object> {
    @Override
    public void accept(Object o) {
        ((FutureLike5Child1) o).await();
    }
}

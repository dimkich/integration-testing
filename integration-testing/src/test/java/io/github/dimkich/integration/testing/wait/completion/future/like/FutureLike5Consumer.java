package io.github.dimkich.integration.testing.wait.completion.future.like;

import eu.ciechanowiec.sneakyfun.SneakyConsumer;

public class FutureLike5Consumer implements SneakyConsumer<Object, Exception> {
    @Override
    public void accept(Object o) {
        ((FutureLike5Child1) o).await();
    }
}

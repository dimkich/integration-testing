package io.github.dimkich.integration.testing.wait.completion.future.like;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;
import lombok.SneakyThrows;

import java.util.concurrent.CountDownLatch;

/**
 * Future-like object whose {@code await} starts a task tracked by
 * {@link io.github.dimkich.integration.testing.wait.completion.MethodCountingAwait} on
 * another thread.
 * <p>
 * The counted method enters before {@code await} returns and finishes a moment later, so the
 * coordinator observes activity from a different subsystem and must run another stabilization
 * step.
 */
public class FutureLike8 {
    private final FutureLike8Task task = new FutureLike8Task();

    /**
     * Starts the counted task and waits until it has entered the intercepted method.
     */
    @SneakyThrows
    public void await() {
        MethodCalls.add(getClass(), "await");
        CountDownLatch entered = new CountDownLatch(1);
        Thread thread = new Thread(() -> task.method(entered));
        thread.start();
        entered.await();
    }
}

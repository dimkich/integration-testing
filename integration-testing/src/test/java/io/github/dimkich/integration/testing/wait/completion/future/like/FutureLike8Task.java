package io.github.dimkich.integration.testing.wait.completion.future.like;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;
import io.github.dimkich.integration.testing.wait.completion.method.counting.MethodCountingTracker;
import lombok.SneakyThrows;

import java.util.concurrent.CountDownLatch;

/**
 * Background task for {@link FutureLike8}: it is intercepted by
 * {@link io.github.dimkich.integration.testing.wait.completion.MethodCountingAwait} and
 * records the in-flight counter value observed inside the method.
 */
public class FutureLike8Task {

    @SneakyThrows
    public void method(CountDownLatch entered) {
        entered.countDown();
        Thread.sleep(100);
        MethodCalls.add(getClass(), "method (count = " + MethodCountingTracker.getActiveTasks() + ")");
    }
}

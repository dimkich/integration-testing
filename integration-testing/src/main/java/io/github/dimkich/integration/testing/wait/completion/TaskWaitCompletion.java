package io.github.dimkich.integration.testing.wait.completion;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * {@link WaitCompletion} implementation that tracks the number of currently running
 * asynchronous tasks and allows callers to block until all of them are finished.
 * <p>
 * The class is thread-safe and uses intrinsic locking with {@code synchronized},
 * {@link #wait(long)} and {@link #notifyAll()}.
 */
@Slf4j
public class TaskWaitCompletion implements WaitCompletion {
    private int taskCount;
    @Getter
    private boolean anyTaskStarted;

    /**
     * Resets the internal task counter and clears the {@link #anyTaskStarted} flag.
     * <p>
     * After calling this method, the next started task will be considered as belonging
     * to a new waiting cycle.
     */
    @Override
    public synchronized void start() {
        anyTaskStarted = false;
        taskCount = 0;
    }

    /**
     * Blocks the current thread until all started tasks are finished or the timeout
     * of 30 seconds elapses.
     * <p>
     * If no tasks are currently running, the method returns immediately.
     */
    @Override
    @SneakyThrows
    public synchronized void waitCompletion() {
        log.trace("{} started wait completion (task count = {})", this, taskCount);
        if (taskCount > 0) {
            wait(30_000);
        }
        log.trace("{} ended wait completion (task count = {})", this, taskCount);
    }

    /**
     * Marks the beginning of a task and increments the internal counter.
     */
    private synchronized void startTask() {
        taskCount++;
        anyTaskStarted = true;
        log.trace("{} started (task count = {})", this, taskCount);
    }

    /**
     * Marks the completion of a task and decrements the internal counter.
     * <p>
     * When the last task completes (counter reaches zero) all waiting threads
     * are notified.
     */
    private synchronized void endTask() {
        if (taskCount > 0) {
            taskCount--;
        }
        log.trace("{} ended (task count = {})", this, taskCount);
        if (taskCount == 0) {
            anyTaskStarted = false;
            notifyAll();
        }
    }

    /**
     * AOP interceptor that should be executed at the beginning of a task.
     * <p>
     * It delegates to {@link TaskWaitCompletion#startTask()} before proceeding
     * with the intercepted method invocation.
     */
    @RequiredArgsConstructor
    public static class Start implements MethodInterceptor {
        private final TaskWaitCompletion taskWaitCompletion;

        /**
         * Invokes {@link TaskWaitCompletion#startTask()} and then proceeds with
         * the original method invocation.
         *
         * @return the result of the original method invocation
         */
        @Nullable
        @Override
        public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
            taskWaitCompletion.startTask();
            return invocation.proceed();
        }
    }

    /**
     * AOP interceptor that should be executed at the end of a task.
     * <p>
     * It always calls {@link TaskWaitCompletion#endTask()} in a {@code finally} block
     * after the intercepted method invocation completes.
     */
    @RequiredArgsConstructor
    public static class End implements MethodInterceptor {
        private final TaskWaitCompletion taskWaitCompletion;

        /**
         * Proceeds with the original method invocation and guarantees that
         * {@link TaskWaitCompletion#endTask()} is called afterwards.
         *
         * @return the result of the original method invocation
         */
        @Nullable
        @Override
        public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
            try {
                return invocation.proceed();
            } finally {
                taskWaitCompletion.endTask();
            }
        }
    }
}
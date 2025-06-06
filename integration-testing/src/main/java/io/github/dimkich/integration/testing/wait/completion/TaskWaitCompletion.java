package io.github.dimkich.integration.testing.wait.completion;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class TaskWaitCompletion implements WaitCompletion {
    private int taskCount;
    @Getter
    private boolean anyTaskStarted;

    @Override
    public synchronized void start() {
        anyTaskStarted = false;
        taskCount = 0;
    }

    @Override
    @SneakyThrows
    public synchronized void waitCompletion() {
        if (taskCount > 0) {
            wait(30_000);
        }
    }

    private synchronized void startTask() {
        taskCount++;
        anyTaskStarted = true;
    }

    private synchronized void endTask() {
        if (taskCount > 0) {
            taskCount--;
        }
        if (taskCount == 0) {
            notifyAll();
        }
    }

    @RequiredArgsConstructor
    public static class Start implements MethodInterceptor {
        private final TaskWaitCompletion taskWaitCompletion;

        @Nullable
        @Override
        public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
            taskWaitCompletion.startTask();
            return invocation.proceed();
        }
    }

    @RequiredArgsConstructor
    public static class End implements MethodInterceptor {
        private final TaskWaitCompletion taskWaitCompletion;

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
package io.github.dimkich.integration.testing.wait.completion;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

@Slf4j
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
        log.trace("{} started wait completion (task count = {})", this, taskCount);
        if (taskCount > 0) {
            wait(30_000);
        }
        log.trace("{} ended wait completion (task count = {})", this, taskCount);
    }

    private synchronized void startTask() {
        taskCount++;
        anyTaskStarted = true;
        log.trace("{} started (task count = {})", this, taskCount);
    }

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
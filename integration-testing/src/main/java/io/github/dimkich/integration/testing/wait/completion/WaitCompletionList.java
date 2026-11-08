package io.github.dimkich.integration.testing.wait.completion;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinates a fixed-point stabilization loop over all registered {@link WaitCompletion}
 * subsystems.
 * <p>
 * Every step invokes {@link WaitCompletion#awaitAndCheckActivity()} on all tasks strictly in
 * parallel, so the duration of a step is the slowest subsystem rather than the sum. When all
 * tasks finish, the coordinator collects their activity flags: if any task performed work, a
 * cascade may still be in progress (for example, a Kafka consumer wrote to Redis), and the
 * whole set is awaited again. The loop ends only when every subsystem reports absolute rest.
 * <p>
 * The pool is sized exactly to {@code tasks.size()}, which excludes starvation and mutual
 * blocking when a task polls or waits synchronously. Any infrastructure exception breaks the
 * loop immediately and is propagated to the test thread as is (fail-fast); the pool is
 * discarded so a task that is still blocked cannot steal a thread from the next test.
 */
@Slf4j
@Getter
public class WaitCompletionList implements DisposableBean {

    /**
     * Wait-completion tasks that should be started and awaited together.
     */
    private final List<WaitCompletion> tasks;

    /**
     * Pool owning exactly {@code tasks.size()} threads; {@code null} for an empty task list.
     */
    private volatile ExecutorService executor;

    public WaitCompletionList(List<WaitCompletion> tasks) {
        this.tasks = tasks;
        this.executor = createExecutor();
    }

    /**
     * Starts all configured wait-completion tasks.
     * <p>
     * This is typically called before {@link #waitCompletion()} to reset activity flags and
     * accumulated errors.
     */
    public void start() {
        tasks.forEach(WaitCompletion::start);
    }

    /**
     * Blocks until all {@link WaitCompletion} tasks have completed and no task has reported
     * new activity in the last step.
     * <p>
     * Each step dispatches {@link WaitCompletion#awaitAndCheckActivity()} to the pool in
     * parallel and joins all results. If any task reports activity, another step is run.
     * The first failure of any task aborts the loop and is rethrown to the caller.
     */
    @SneakyThrows
    public void waitCompletion() {
        if (tasks.isEmpty()) {
            return;
        }
        while (true) {
            log.debug("Waiting for completion of tasks.");
            ExecutorService pool = executor();
            List<CompletableFuture<Boolean>> futures = new ArrayList<>(tasks.size());
            for (WaitCompletion task : tasks) {
                futures.add(CompletableFuture.supplyAsync(task::awaitAndCheckActivity, pool));
            }
            try {
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            } catch (CompletionException e) {
                discardExecutor();
                throw unwrap(e);
            }
            boolean anyActivity = false;
            for (CompletableFuture<Boolean> future : futures) {
                anyActivity |= future.join();
            }
            if (!anyActivity) {
                log.debug("All tasks completed.");
                return;
            }
        }
    }

    /**
     * Releases the pool when the Spring context stops.
     */
    @Override
    public void destroy() {
        discardExecutor();
    }

    private ExecutorService executor() {
        ExecutorService pool = executor;
        if (pool == null || pool.isShutdown()) {
            synchronized (this) {
                if (executor == null || executor.isShutdown()) {
                    executor = createExecutor();
                }
                pool = executor;
            }
        }
        return pool;
    }

    private ExecutorService createExecutor() {
        if (tasks.isEmpty()) {
            return null;
        }
        AtomicInteger counter = new AtomicInteger();
        return Executors.newFixedThreadPool(tasks.size(), runnable -> {
            Thread thread = new Thread(runnable, "wait-completion-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    private void discardExecutor() {
        synchronized (this) {
            ExecutorService pool = executor;
            executor = null;
            if (pool != null) {
                pool.shutdownNow();
            }
        }
    }

    /**
     * Removes the wrapping added by {@link CompletableFuture} so the test receives the
     * original exception thrown by a subsystem.
     */
    private static Throwable unwrap(Throwable throwable) {
        Throwable cause = throwable;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}

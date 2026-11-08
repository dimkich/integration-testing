package io.github.dimkich.integration.testing.wait.completion;

/**
 * Abstraction for synchronizing a test with asynchronous work of one subsystem.
 * <p>
 * The lifecycle is driven by {@link WaitCompletionList}:
 * <ol>
 *   <li>{@link #start()} is called before the test action (or before test initialization).
 *       It only resets accumulated errors and activity flags and must not block.</li>
 *   <li>{@link #awaitAndCheckActivity()} is called repeatedly in a stabilization loop: all
 *       registered subsystems are awaited in parallel, and the loop repeats while at least
 *       one of them reports activity.</li>
 * </ol>
 * Activity is measured as a window between coordinator phases: {@link #start()} opens the
 * first window, and every {@link #awaitAndCheckActivity()} call atomically returns the
 * activity accumulated since {@link #start()} or since the previous return and opens a new
 * window. An event that arrives while a step is running is therefore never lost: it is
 * reported by the next call and makes the coordinator run another stabilization step.
 */
public interface WaitCompletion {

    /**
     * Marks the beginning of the waiting lifecycle.
     * <p>
     * Implementations reset their counters, accumulated errors and primary activity flags.
     * This method must not block waiting for external readiness (started consumers,
     * connections and the like): all waiting belongs to {@link #awaitAndCheckActivity()}.
     */
    void start();

    /**
     * Blocks the current thread until the subsystem reaches local quiescence and atomically
     * takes the activity accumulated since {@link #start()} or the previous return.
     * <p>
     * Implementations fail fast on infrastructure errors (a wait timeout, a dead replicator)
     * and are free to use {@code @SneakyThrows} for checked interrupts.
     *
     * @return {@code true} if the subsystem observed activity in the current window,
     *         {@code false} if it was in absolute rest
     */
    boolean awaitAndCheckActivity();
}

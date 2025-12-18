package io.github.dimkich.integration.testing.wait.completion;

/**
 * Abstraction for waiting until asynchronous tasks are completed.
 * <p>
 * Typical usage:
 * <ol>
 *   <li>Call {@link #start()} before the asynchronous work is triggered.</li>
 *   <li>Optionally check {@link #isAnyTaskStarted()} to see if anything has begun.</li>
 *   <li>Call {@link #waitCompletion()} to block until all tasks are finished.</li>
 * </ol>
 */
public interface WaitCompletion {

    /**
     * Marks the beginning of the waiting lifecycle.
     * <p>
     * Implementations may use this to start tracking asynchronous tasks or register
     * listeners that will be awaited in {@link #waitCompletion()}.
     */
    void start();

    /**
     * Indicates whether at least one asynchronous task has been started.
     *
     * @return {@code true} if any task has started and is being tracked, {@code false} otherwise
     */
    boolean isAnyTaskStarted();

    /**
     * Blocks the current thread until all tracked asynchronous tasks are completed.
     * <p>
     * Implementations should guarantee that after this method returns,
     * there are no pending tasks associated with this instance.
     */
    void waitCompletion();
}

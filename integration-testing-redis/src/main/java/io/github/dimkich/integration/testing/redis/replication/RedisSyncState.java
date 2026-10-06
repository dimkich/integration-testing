package io.github.dimkich.integration.testing.redis.replication;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Live synchronization state of one Redis connection: whether live command sync (the STREAM
 * phase) is running and the last fatal replication error.
 * <p>
 * Written by the replication infrastructure: {@link io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSyncStateDelegator}
 * activates and deactivates it on sync-phase transitions and records processing errors;
 * {@link RedisReplicatorFactory} records connection errors via an exception listener.
 * <p>
 * Observed by the test side: {@link io.github.dimkich.integration.testing.redis.RedisWaitCompletion}
 * awaits activation before synchronizing, and {@link RedisSyncBarrier} consults
 * {@link #throwFatalError()} before publishing a token. A registered
 * {@link #onFatalError(Consumer) failure handler} lets the barrier abort an in-flight token wait.
 */
@RequiredArgsConstructor
public class RedisSyncState {
    private final String name;
    private final Object monitor = new Object();
    private volatile boolean active;
    private volatile Throwable fatalError;
    private volatile Consumer<Throwable> failureHandler;

    /** Switches to live command sync and wakes everyone waiting in {@link #awaitActive(long)}. */
    public void activate() {
        synchronized (monitor) {
            this.active = true;
            monitor.notifyAll();
        }
    }

    /** Switches away from live command sync; {@link #awaitActive(long)} starts blocking again. */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Records a fatal replication error: wakes {@link #awaitActive(long)} and notifies the
     * failure handler so an in-flight token wait can be aborted.
     *
     * @param throwable error from replication; rethrown on the next wait
     */
    public void setFatalError(Throwable throwable) {
        this.fatalError = throwable;
        synchronized (monitor) {
            monitor.notifyAll();
        }
        Consumer<Throwable> handler = failureHandler;
        if (handler != null) {
            handler.accept(throwable);
        }
    }

    /**
     * Registers the handler invoked on every {@link #setFatalError(Throwable)}.
     *
     * @param handler failure handler
     */
    public void onFatalError(Consumer<Throwable> handler) {
        this.failureHandler = handler;
    }

    /**
     * Waits until live command sync is running.
     *
     * @param timeoutMs maximum wait in milliseconds
     * @throws RuntimeException if command sync does not resume, or a fatal replication error
     *                          is pending
     */
    @SneakyThrows
    public void awaitActive(long timeoutMs) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        synchronized (monitor) {
            while (!active) {
                throwFatalError();
                long remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0) {
                    throw new RuntimeException("Redis Sync Timeout for [" + name + "]: command sync did not resume");
                }
                monitor.wait(TimeUnit.NANOSECONDS.toMillis(remainingNanos) + 1);
            }
        }
    }

    /**
     * Throws and clears the pending fatal replication error, if any.
     */
    @SneakyThrows
    public void throwFatalError() {
        Throwable error = fatalError;
        if (error != null) {
            fatalError = null;
            throw error;
        }
    }
}

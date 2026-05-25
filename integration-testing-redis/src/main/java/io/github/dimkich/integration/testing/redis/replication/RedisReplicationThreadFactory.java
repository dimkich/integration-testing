package io.github.dimkich.integration.testing.redis.replication;

import lombok.NonNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link ThreadFactory} for Redis replication worker threads used by {@link RedisSyncManager}.
 * <p>
 * Threads are named {@code redis-repl-<n>} (monotonically increasing {@code n}) and marked as
 * daemon, so they do not block JVM shutdown when the test context stops.
 *
 * @see RedisSyncManager
 */
public class RedisReplicationThreadFactory implements ThreadFactory {
    private final AtomicInteger count = new AtomicInteger(0);

    /**
     * Creates a new daemon thread that runs {@code r}.
     *
     * @param r task executed by the new thread
     * @return a daemon thread named {@code redis-repl-<n>}
     */
    @Override
    public Thread newThread(@NonNull Runnable r) {
        Thread thread = new Thread(r, "redis-repl-" + count.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }
}

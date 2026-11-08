package io.github.dimkich.integration.testing.redis.replication;

import com.moilioncircle.redis.replicator.Replicator;
import eu.ciechanowiec.sneakyfun.SneakyRunnable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Spring {@link SmartLifecycle} that opens and closes all {@link Replicator} beans when the test
 * application context starts and stops.
 * <p>
 * Imported via {@link io.github.dimkich.integration.testing.redis.config.RedisConfig} and
 * constructed with every {@link com.moilioncircle.redis.replicator.Replicator} in the context (one per
 * configured connection, created by
 * {@link RedisReplicatorFactory} in
 * {@link io.github.dimkich.integration.testing.redis.config.RedisConfig.PostProcessor}).
 * On {@link #start()}, each replicator's {@link Replicator#open()} runs on a dedicated daemon
 * worker thread ({@link RedisReplicationThreadFactory}); {@link #stop()} closes replicators and
 * shuts down the pool.
 * <p>
 * {@link #getPhase()} returns {@link Integer#MIN_VALUE} so replication starts before most other
 * lifecycle beans, keeping the in-memory mirror ready for tests.
 *
 * @see RedisReplicatorFactory
 * @see RedisReplicationThreadFactory
 */
@Slf4j
@RequiredArgsConstructor
public class RedisSyncManager implements SmartLifecycle {
    /** How long {@link #stop()} waits for replication threads to finish. */
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final List<Replicator> replicators;

    private ExecutorService executor;
    private volatile boolean running = false;

    /**
     * Opens every replicator on a fixed thread pool, or marks running with no pool when the list
     * is empty.
     * <p>
     * {@link Replicator#open()} blocks for the lifetime of the stream, so its outcome cannot be
     * awaited here. A start-up failure is reported to the connection's
     * {@link RedisSyncState} by {@link FastHeartbeatReplicator#open()} and rethrown by the next
     * synchronization wait instead of being lost in the worker's {@code Future}.
     */
    @Override
    public void start() {
        if (replicators.isEmpty()) {
            this.running = true;
            return;
        }

        this.executor = Executors.newFixedThreadPool(replicators.size(), new RedisReplicationThreadFactory());

        replicators.forEach(replicator ->
                executor.submit(SneakyRunnable.sneaky(replicator::open)));

        this.running = true;
    }

    /** Closes all replicators and shuts down the replication executor, if any. */
    @Override
    public void stop() {
        replicators.forEach(replicator -> {
            try {
                replicator.close();
            } catch (IOException e) {
                log.debug("Failed to close Redis replicator: {}", e.getMessage());
            }
        });
        if (executor != null) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    log.warn("Redis replication threads did not stop within {} seconds",
                            SHUTDOWN_TIMEOUT_SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for Redis replication threads to stop");
            }
            executor = null;
        }
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@link Integer#MIN_VALUE} so this lifecycle starts as early as possible.
     */
    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}


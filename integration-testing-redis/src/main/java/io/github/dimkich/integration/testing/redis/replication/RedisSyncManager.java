package io.github.dimkich.integration.testing.redis.replication;

import com.moilioncircle.redis.replicator.Replicator;
import eu.ciechanowiec.sneakyfun.SneakyRunnable;
import lombok.RequiredArgsConstructor;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Spring {@link SmartLifecycle} that opens and closes all {@link Replicator} beans when the test
 * application context starts and stops.
 * <p>
 * Imported via {@link io.github.dimkich.integration.testing.redis.config.RedisConfig} and
 * constructed with every {@link Replicator} in the context (one per
 * {@link org.springframework.data.redis.connection.RedisConnectionFactory}, created by
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
@RequiredArgsConstructor
public class RedisSyncManager implements SmartLifecycle {
    private final List<Replicator> replicators;

    private ExecutorService executor;
    private volatile boolean running = false;

    /**
     * Opens every replicator on a fixed thread pool, or marks running with no pool when the list
     * is empty.
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
        replicators.forEach(r -> {
            try {
                r.close();
            } catch (IOException ignored) {
            }
        });
        if (executor != null) {
            executor.shutdownNow();
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


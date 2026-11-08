package io.github.dimkich.integration.testing.redis;

import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.RedisSyncBarrier;
import io.github.dimkich.integration.testing.redis.replication.RedisSyncState;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import lombok.RequiredArgsConstructor;

/**
 * {@link WaitCompletion} implementation for Redis: waits until live command sync is running
 * ({@link RedisSyncState}) and the replication stream of one connection has caught up to a
 * barrier token ({@link RedisSyncBarrier}). It is the single synchronization point for the
 * module: the test lifecycle invokes it after initialization (so data storage snapshots and
 * diffs are read from a caught-up mirror) and after the test action (so pushes and mutations
 * are captured before assertions).
 * <p>
 * One instance is registered per configured connection
 * ({@link io.github.dimkich.integration.testing.redis.config.RedisConfig}). {@link #start()}
 * resets the activity sensor of the connection's in-memory mirror; the actual waiting happens
 * in {@link #awaitAndCheckActivity()}, which pumps the barrier and then atomically takes the
 * activity accumulated since {@link #start()} or the previous step.
 * <p>
 * The SUT processes published messages asynchronously; tests must explicitly await the SUT
 * side (for example with a facade) before asserting on it, the barrier only guarantees that
 * the replication stream has caught up with the broker.
 */
@RequiredArgsConstructor
public class RedisWaitCompletion implements WaitCompletion {
    private final RedisSyncState state;
    private final RedisSyncBarrier barrier;
    private final RedisInMemoryStore store;
    private final RedisProperties properties;

    /** Resets the activity sensor of the connection before a new test. */
    @Override
    public void start() {
        store.resetActivity();
    }

    /**
     * Pumps the replication barrier (so the local mirror is complete) and takes the activity
     * of the commands applied since the previous step.
     *
     * @return {@code true} if the application changed at least one key or published a message
     */
    @Override
    public boolean awaitAndCheckActivity() {
        state.awaitActive(properties.getSyncBarrierTimeoutMs());
        barrier.triggerAndAwait(properties.getSyncBarrierTimeoutMs());
        state.throwFatalError();
        return store.takeActivity();
    }
}

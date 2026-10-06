package io.github.dimkich.integration.testing.redis;

import io.github.dimkich.integration.testing.redis.config.RedisProperties;
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
 * One instance is registered per {@link org.springframework.data.redis.connection.RedisConnectionFactory}
 * ({@link io.github.dimkich.integration.testing.redis.config.RedisConfig}). The waiting happens
 * in {@link #waitCompletion()}; there is no per-test state to reset in {@link #start()}.
 * <p>
 * The SUT processes published messages asynchronously; tests must explicitly await the SUT
 * side (for example with a facade) before asserting on it, the barrier only guarantees that
 * the replication stream has caught up with the broker.
 */
@RequiredArgsConstructor
public class RedisWaitCompletion implements WaitCompletion {
    private final RedisSyncState state;
    private final RedisSyncBarrier barrier;
    private final RedisProperties properties;

    /** No per-test state: synchronization happens in {@link #waitCompletion()}. */
    @Override
    public void start() {
    }

    @Override
    public boolean isAnyTaskStarted() {
        return false;
    }

    @Override
    public void waitCompletion() {
        state.awaitActive(properties.getSyncBarrierTimeoutMs());
        barrier.triggerAndAwait(properties.getSyncBarrierTimeoutMs());
    }
}

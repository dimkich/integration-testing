package io.github.dimkich.integration.testing.redis.replication;

import com.moilioncircle.redis.replicator.Configuration;
import com.moilioncircle.redis.replicator.RedisSocketReplicator;
import io.github.dimkich.integration.testing.redis.client.RedisClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * {@link RedisSocketReplicator} used by {@link RedisReplicatorFactory} for integration tests.
 * <p>
 * The stock replicator schedules the first {@code REPLCONF ACK} after
 * {@link Configuration#getHeartbeatPeriod()}; this subclass uses an initial delay of zero so the
 * replica reports its offset as soon as the heartbeat task starts. Subsequent ACKs still run at
 * {@code heartbeatPeriod} intervals, which keeps the master informed while reducing startup latency
 * when waiting for replication to catch up.
 * <p>
 * The replicator opens on a worker thread (see
 * {@link RedisSyncManager}), so a failure of {@link #open()} would otherwise be lost in an
 * unread {@code Future}. This subclass reports it to {@link RedisSyncState}, where the next
 * sync wait rethrows it instead of a generic timeout.
 *
 * @see RedisReplicatorFactory#createReplicator(String, RedisClient, RedisSyncState)
 */
public class FastHeartbeatReplicator extends RedisSocketReplicator {
    private final RedisSyncState state;

    /**
     * @param host          Redis master host
     * @param port          Redis master port
     * @param configuration replicator settings (auth, offset, heartbeat period, etc.)
     * @param state         sync state that receives a fatal error when {@link #open()} fails
     */
    public FastHeartbeatReplicator(String host, int port, Configuration configuration, RedisSyncState state) {
        super(host, port, configuration);
        this.state = state;
    }

    /**
     * Opens the replication stream; a failure is recorded in the sync state before it is rethrown
     * to the worker thread.
     */
    @Override
    public void open() throws IOException {
        try {
            super.open();
        } catch (IOException | RuntimeException | Error e) {
            state.setFatalError(e);
            throw e;
        }
    }

    /**
     * Starts periodic {@code REPLCONF ACK} heartbeats with no initial delay.
     */
    @Override
    protected void heartbeat() {
        assert heartbeat == null || heartbeat.isCancelled();
        heartbeat = executor.scheduleWithFixedDelay(() -> sendQuietly("REPLCONF".getBytes(), "ACK".getBytes(),
                        String.valueOf(configuration.getReplOffset()).getBytes()), 0,
                configuration.getHeartbeatPeriod(), TimeUnit.MILLISECONDS);
    }
}


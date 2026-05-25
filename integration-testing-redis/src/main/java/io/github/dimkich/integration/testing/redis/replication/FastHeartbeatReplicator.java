package io.github.dimkich.integration.testing.redis.replication;

import com.moilioncircle.redis.replicator.Configuration;
import com.moilioncircle.redis.replicator.RedisSocketReplicator;

import java.util.concurrent.TimeUnit;

/**
 * {@link RedisSocketReplicator} used by {@link RedisReplicatorFactory} for integration tests.
 * <p>
 * The stock replicator schedules the first {@code REPLCONF ACK} after
 * {@link Configuration#getHeartbeatPeriod()}; this subclass uses an initial delay of zero so the
 * replica reports its offset as soon as the heartbeat task starts. Subsequent ACKs still run at
 * {@code heartbeatPeriod} intervals, which keeps the master informed while reducing startup latency
 * when waiting for replication to catch up.
 *
 * @see RedisReplicatorFactory#createReplicator(String, org.springframework.data.redis.connection.RedisConnectionFactory, RedisSyncBarrier)
 */
public class FastHeartbeatReplicator extends RedisSocketReplicator {

    /**
     * @param host          Redis master host
     * @param port          Redis master port
     * @param configuration replicator settings (auth, offset, heartbeat period, etc.)
     */
    public FastHeartbeatReplicator(String host, int port, Configuration configuration) {
        super(host, port, configuration);
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


package io.github.dimkich.integration.testing.redis.replication;

import com.moilioncircle.redis.replicator.Configuration;
import com.moilioncircle.redis.replicator.RedisSocketReplicator;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.cmd.CommandName;
import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;

import java.util.List;

/**
 * Builds and configures a {@link Replicator} for a named Redis connection in integration tests.
 * <p>
 * Spring registers one factory bean and invokes {@link #createReplicator(String, RedisConnectionFactory, RedisSyncBarrier)}
 * per {@link org.springframework.data.redis.connection.RedisConnectionFactory} (see
 * {@link io.github.dimkich.integration.testing.redis.config.RedisConfig}). Before connecting, the
 * target Redis instance is tuned for fast, in-process replication ({@code repl-diskless-sync},
 * zero output-buffer limits for replicas). The returned replicator is a {@link FastHeartbeatReplicator}
 * with auth from {@link RedisProperties}, all {@link NamedCommandParser} beans registered, and
 * replication errors forwarded to the sync barrier.
 *
 * @see FastHeartbeatReplicator
 * @see NamedCommandParser
 */
@RequiredArgsConstructor
public class RedisReplicatorFactory {
    private final RedisProperties properties;
    private final List<NamedCommandParser<?>> customParsers;

    /**
     * Creates a socket replicator for the connection named {@code factoryName}.
     *
     * @param factoryName        connection name as defined in {@link RedisProperties}
     * @param connectionFactory  factory used to apply server config and reach the Redis instance
     * @param barrier            receives fatal replication errors via an exception listener
     * @return configured {@link Replicator} ready to {@link Replicator#open()}
     * @throws IllegalStateException if host is missing or port is not positive
     */
    public Replicator createReplicator(String factoryName,
                                       RedisConnectionFactory connectionFactory,
                                       RedisSyncBarrier barrier) {
        RedisProperties.Connection conn = properties.getConnection(factoryName);
        if (conn.getHost() == null || conn.getHost().isEmpty()) {
            throw new IllegalStateException("Connection host is missing.");
        }
        if (conn.getPort() <= 0) {
            throw new IllegalStateException(String.format("Connection port is invalid: %d", conn.getPort()));
        }

        prepareRedisServer(connectionFactory);

        Configuration config = Configuration.defaultSetting()
                .setAuthPassword(conn.getPassword())
                .setAuthUser(conn.getUser());

        RedisSocketReplicator replicator = new FastHeartbeatReplicator(
                conn.getHost(),
                conn.getPort(),
                config
        );

        for (NamedCommandParser<?> parser : customParsers) {
            replicator.addCommandParser(CommandName.name(parser.getCommandName()), parser);
        }
        replicator.addExceptionListener((rep, ex, event) -> barrier.setFatalError(ex));

        return replicator;
    }

    /**
     * Enables diskless replication and removes replica output-buffer limits so the in-memory
     * replica can keep up without blocking on RDB files or buffer backpressure.
     */
    private void prepareRedisServer(RedisConnectionFactory connectionFactory) {
        try (RedisConnection conn = connectionFactory.getConnection()) {
            RedisServerCommands server = conn.serverCommands();
            server.setConfig("repl-diskless-sync", "yes");
            server.setConfig("repl-diskless-sync-delay", "0");
            server.setConfig("client-output-buffer-limit", "replica 0 0 0");
        }
    }
}
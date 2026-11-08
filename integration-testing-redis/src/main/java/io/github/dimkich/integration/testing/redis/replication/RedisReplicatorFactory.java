package io.github.dimkich.integration.testing.redis.replication;

import com.moilioncircle.redis.replicator.Configuration;
import com.moilioncircle.redis.replicator.RedisSocketReplicator;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.cmd.CommandName;
import io.github.dimkich.integration.testing.redis.client.RedisClient;
import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import lombok.RequiredArgsConstructor;

import javax.net.ssl.SSLSocketFactory;
import java.util.List;

/**
 * Builds and configures a {@link Replicator} for a named Redis connection in integration tests.
 * <p>
 * Spring registers one replicator bean per configured connection and invokes
 * {@link #createReplicator(String, RedisClient, RedisSyncState)}
 * (see {@link io.github.dimkich.integration.testing.redis.config.RedisConfig}). Before connecting,
 * the target Redis instance is tuned for fast, in-process replication ({@code repl-diskless-sync},
 * zero output-buffer limits for replicas). The returned replicator is a
 * {@link FastHeartbeatReplicator} with auth, transport settings ({@code timeoutMs}, {@code ssl})
 * and all {@link NamedCommandParser} beans registered, and replication errors forwarded to the
 * sync state.
 *
 * @see FastHeartbeatReplicator
 * @see NamedCommandParser
 */
@RequiredArgsConstructor
public class RedisReplicatorFactory {
    private final RedisProperties properties;
    private final List<NamedCommandParser<?>> customParsers;

    /**
     * Creates a socket replicator for the connection named {@code connectionName}.
     *
     * @param connectionName connection name as defined in {@link RedisProperties}
     * @param client         framework client used to tune the server
     * @param state          receives fatal replication errors via an exception listener
     * @return configured {@link Replicator} ready to {@link Replicator#open()}
     * @throws IllegalStateException if host is missing or port is not positive
     */
    public Replicator createReplicator(String connectionName,
                                       RedisClient client,
                                       RedisSyncState state) {
        RedisProperties.Connection conn = properties.getConnection(connectionName);
        if (conn.getHost() == null || conn.getHost().isEmpty()) {
            throw new IllegalStateException("Redis host is missing for connection [" + connectionName + "]");
        }
        if (conn.getPort() == null || conn.getPort() <= 0) {
            throw new IllegalStateException(String.format(
                    "Redis port is invalid for connection [%s]: %s", connectionName, conn.getPort()));
        }

        prepareRedisServer(client);

        int timeout = conn.getTimeoutMs();
        boolean ssl = conn.getSsl();
        Configuration config = Configuration.defaultSetting()
                .setAuthPassword(conn.getPassword())
                .setAuthUser(conn.getUser())
                .setConnectionTimeout(timeout)
                .setReadTimeout(timeout)
                .setSsl(ssl);
        if (ssl) {
            config.setSslSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
        }

        RedisSocketReplicator replicator = new FastHeartbeatReplicator(
                conn.getHost(),
                conn.getPort(),
                config,
                state
        );

        for (NamedCommandParser<?> parser : customParsers) {
            replicator.addCommandParser(CommandName.name(parser.getCommandName()), parser);
        }
        replicator.addExceptionListener((rep, ex, event) -> state.setFatalError(ex));

        return replicator;
    }

    /**
     * Enables diskless replication and removes replica output-buffer limits so the in-memory
     * replica can keep up without blocking on RDB files or buffer backpressure.
     */
    private void prepareRedisServer(RedisClient client) {
        client.configSet("repl-diskless-sync", "yes");
        client.configSet("repl-diskless-sync-delay", "0");
        client.configSet("client-output-buffer-limit", "replica 0 0 0");
    }
}

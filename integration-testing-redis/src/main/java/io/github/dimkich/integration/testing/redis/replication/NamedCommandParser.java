package io.github.dimkich.integration.testing.redis.replication;

import com.moilioncircle.redis.replicator.cmd.Command;
import com.moilioncircle.redis.replicator.cmd.CommandParser;

/**
 * {@link CommandParser} that exposes the Redis command name used to register it with a replicator.
 * <p>
 * Custom parsers (for example {@code ZRANGESTORE}) are collected as Spring beans and registered in
 * {@link RedisReplicatorFactory#createReplicator(String, org.springframework.data.redis.connection.RedisConnectionFactory, RedisSyncBarrier)}
 * via {@link com.moilioncircle.redis.replicator.Replicator#addCommandParser}.
 *
 * @param <T> parsed command type
 * @see RedisReplicatorFactory
 */
public interface NamedCommandParser<T extends Command> extends CommandParser<T> {

    /**
     * Returns the Redis command name (e.g. {@code "ZRANGESTORE"}) passed to
     * {@link com.moilioncircle.redis.replicator.cmd.CommandName#name(String)} when registering this parser.
     *
     * @return uppercase command name
     */
    String getCommandName();
}

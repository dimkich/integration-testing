package io.github.dimkich.integration.testing.redis.replication.event.listener;

import com.moilioncircle.redis.replicator.cmd.Command;

/**
 * Handler for live command replication events (incremental sync / {@code PreCommandSyncEvent} phase).
 * <p>
 * Beans implementing this interface are wired into the stream
 * {@link io.github.dimkich.integration.testing.redis.replication.RedisEventDispatcher}.
 *
 * @param <E> replicated Redis command type
 */
public interface RedisStreamHandler<E extends Command> extends RedisEventHandler<E> {
}

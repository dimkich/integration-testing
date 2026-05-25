package io.github.dimkich.integration.testing.redis.replication.event.listener;

import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;

/**
 * Applies a single replication {@link Event} to an in-memory {@link RedisInMemoryStore}.
 * <p>
 * Implementations are registered as Spring beans and collected by
 * {@link io.github.dimkich.integration.testing.redis.replication.RedisEventDispatcher}.
 * Use {@link RedisSnapshotHandler} or {@link RedisStreamHandler} to mark the sync phase
 * the handler belongs to.
 *
 * @param <E> concrete event type this handler processes
 */
public interface RedisEventHandler<E extends Event> {

    /**
     * @param eventClass runtime class of the incoming event
     * @return {@code true} if this handler should process events of that type
     */
    boolean canHandle(Class<? extends Event> eventClass);

    /**
     * Mutates {@code store} to reflect the replication event.
     *
     * @param event the replicator event
     * @param store target in-memory Redis state for the current connection
     */
    void handle(E event, RedisInMemoryStore store);
}

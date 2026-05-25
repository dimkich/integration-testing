package io.github.dimkich.integration.testing.redis.replication.event.listener;

import com.moilioncircle.redis.replicator.event.Event;

/**
 * Handler for RDB snapshot replication events (full sync / {@code PreRdbSyncEvent} phase).
 * <p>
 * Beans implementing this interface are wired into the snapshot
 * {@link io.github.dimkich.integration.testing.redis.replication.RedisEventDispatcher}.
 *
 * @param <E> snapshot event type (e.g. {@code KeyStringValueString})
 */
public interface RedisSnapshotHandler<E extends Event> extends RedisEventHandler<E> {
}

package io.github.dimkich.integration.testing.redis.replication.event.listener;

import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;

/**
 * Fallback handler used when replication is between sync phases.
 * <p>
 * Any data event received in this phase indicates a protocol or lifecycle bug and fails fast.
 */
public class UnknownPhaseListener implements RedisEventHandler<Event> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return true;
    }

    @Override
    public void handle(Event event, RedisInMemoryStore store) {
        throw new IllegalStateException("Protocol violation: data received during UNKNOWN phase: "
                + event.getClass().getName());
    }
}

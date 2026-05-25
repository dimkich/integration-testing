package io.github.dimkich.integration.testing.redis.replication.event.listener;

import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.event.*;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.RedisSyncBarrier;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Replicator {@link EventListener} that routes events by Redis sync phase.
 * <p>
 * On {@code PreRdbSyncEvent} the store is flushed and snapshot handlers take over; on
 * {@code PreCommandSyncEvent} the {@link RedisSyncBarrier} is activated and stream handlers
 * process live commands; between phases {@link UnknownPhaseListener} rejects stray data.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisSyncStateDelegator implements EventListener {
    private final RedisInMemoryStore memStore;
    private final RedisSyncBarrier barrier;
    private final UnknownPhaseListener unknownPhaseListener;
    private final RedisEventHandler<Event> snapshotDispatcher;
    private final RedisEventHandler<Event> streamDispatcher;

    private volatile RedisEventHandler<Event> currentHandler;

    @PostConstruct
    public void init() {
        currentHandler = unknownPhaseListener;
        memStore.addEventListener(this);
    }

    @Override
    public void onEvent(Replicator replicator, Event event) {
        try {
            if (event instanceof PreRdbSyncEvent) {
                log.trace("Redis [{}]: Full sync detected (RDB start). Switching to SNAPSHOT.", memStore.getName());
                this.switchToSnapshot();
                return;
            }
            if (event instanceof PreCommandSyncEvent) {
                log.trace("Redis [{}]: Command sync start. Switching to STREAM.", memStore.getName());
                this.switchToStream();
                return;
            }
            if (event instanceof PostRdbSyncEvent || event instanceof PostCommandSyncEvent) {
                log.trace("Redis [{}]: Sync phase ended. Switching to UNKNOWN.", memStore.getName());
                this.switchToUnknown();
                return;
            }
            log.trace("Receive event " + memStore.getName() + " " + event);
            currentHandler.handle(event, memStore);
        } catch (Throwable e) {
            barrier.setFatalError(e);
            log.error("Error during event processing: {}", event, e);
        }
    }

    /** Clears the store and delegates subsequent data events to snapshot handlers. */
    public void switchToSnapshot() {
        memStore.flushAll();
        barrier.deactivate();
        this.currentHandler = snapshotDispatcher;
    }

    /** Deactivates the sync barrier and rejects data until the next phase begins. */
    public void switchToUnknown() {
        barrier.deactivate();
        this.currentHandler = unknownPhaseListener;
    }

    /** Enables the sync barrier and delegates subsequent command events to stream handlers. */
    public void switchToStream() {
        barrier.activate();
        this.currentHandler = streamDispatcher;
    }
}

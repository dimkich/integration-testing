package io.github.dimkich.integration.testing.redis.replication;

import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisEventHandler;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Composite {@link RedisEventHandler} that routes each replication {@link Event} to the first
 * delegate in {@code list} whose {@link RedisEventHandler#canHandle(Class)} returns {@code true}.
 * <p>
 * Spring creates two beans of this type in {@link io.github.dimkich.integration.testing.redis.config.RedisConfig}:
 * {@code snapshotDispatcher} (collects {@link io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler}
 * beans) and {@code streamDispatcher} (collects
 * {@link io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler} beans).
 * {@link io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSyncStateDelegator}
 * selects the active dispatcher by sync phase.
 * <p>
 * Handler lookup is cached per event class so repeated events of the same type avoid scanning
 * {@code list}.
 */
@RequiredArgsConstructor
public class RedisEventDispatcher implements RedisEventHandler<Event> {
    private final List<? extends RedisEventHandler<? extends Event>> list;
    private final Map<Class<?>, RedisEventHandler<Event>> cache = new ConcurrentHashMap<>();

    /**
     * Always {@code true}: this dispatcher accepts every event and delegates matching to
     * {@link #list}.
     */
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return true;
    }

    /**
     * Resolves a delegate for {@code event.getClass()} (using {@link #cache}) and applies it to
     * {@code store}.
     *
     * @param event replication event from the replicator
     * @param store in-memory Redis state for the current connection
     * @throws UnsupportedOperationException if no delegate supports the event type
     */
    @Override
    @SuppressWarnings("unchecked")
    public void handle(Event event, RedisInMemoryStore store) {
        cache.computeIfAbsent(event.getClass(), k -> (RedisEventHandler<Event>) list.stream()
                        .filter(e -> e.canHandle(event.getClass()))
                        .findFirst()
                        .orElseThrow(() -> createUnsupportedException(event.getClass())))
                .handle(event, store);
    }

    /**
     * Builds an {@link UnsupportedOperationException} for an unhandled event type, including hints
     * for Redis modules, raw RDB dumps, and missing snapshot/stream handlers.
     */
    private UnsupportedOperationException createUnsupportedException(Class<?> clazz) {
        String className = clazz.getName();
        StringBuilder message = new StringBuilder("No handler for ").append(className).append(" ");

        if (className.contains("KeyStringValueModule")) {
            message.append("Hint: This is a Redis Module (e.g. RedisJSON). ")
                    .append("Implement a custom RedisSnapshotHandler to support it.");
        } else if (className.contains("DumpKeyValuePair")) {
            message.append("Hint: Received a raw RDB dump. ")
                    .append("This happens if the Redis version is too new or the format is unknown. ")
                    .append("You may need to update the replicator library or add a custom handler.");
        } else {
            message.append("Hint: If this is a standard Redis feature, consider implementing a handler ")
                    .append("RedisSnapshotHandler/RedisStreamHandler");
        }

        return new UnsupportedOperationException(message.toString());
    }
}

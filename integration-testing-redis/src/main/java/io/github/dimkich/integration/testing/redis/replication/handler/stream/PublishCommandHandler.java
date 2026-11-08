package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.PublishCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.message.RedisPushCapture;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublishCommandHandler implements RedisStreamHandler<PublishCommand> {
    private final RedisPushCapture redisPushCapture;

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == PublishCommand.class;
    }

    @Override
    public void handle(PublishCommand event, RedisInMemoryStore store) {
        if (!store.getBarrier().isSyncChannel(event.getChannel())) {
            // Service traffic of the barrier must not wake the activity sensor, otherwise
            // the coordinator would never converge.
            store.markActivity();
        }
        store.getBarrier().release(event.getChannel(), event.getMessage());
        redisPushCapture.capture(store, event.getChannel(), event.getMessage());
    }
}

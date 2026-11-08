package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.SPublishCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.message.RedisPushCapture;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SPublishCommandHandler implements RedisStreamHandler<SPublishCommand> {
    private final RedisPushCapture redisPushCapture;

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == SPublishCommand.class;
    }

    @Override
    public void handle(SPublishCommand event, RedisInMemoryStore store) {
        if (!store.getBarrier().isSyncChannel(event.getChannel())) {
            // Service traffic of the barrier must not wake the activity sensor.
            store.markActivity();
        }
        redisPushCapture.capture(store, event.getChannel(), event.getMessage());
    }
}

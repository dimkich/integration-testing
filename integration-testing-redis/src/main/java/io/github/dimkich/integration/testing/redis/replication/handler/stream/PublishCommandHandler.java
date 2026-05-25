package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.PublishCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PublishCommandHandler implements RedisStreamHandler<PublishCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == PublishCommand.class;
    }

    @Override
    public void handle(PublishCommand event, RedisInMemoryStore store) {
        String channel = new String(event.getChannel(), StandardCharsets.UTF_8);
        String message = new String(event.getMessage(), StandardCharsets.UTF_8);
        store.getBarrier().release(channel, message);
    }
}

package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.FlushAllCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class FlushAllCommandHandler implements RedisStreamHandler<FlushAllCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == FlushAllCommand.class;
    }

    @Override
    public void handle(FlushAllCommand event, RedisInMemoryStore store) {
        store.flushAll();
    }
}

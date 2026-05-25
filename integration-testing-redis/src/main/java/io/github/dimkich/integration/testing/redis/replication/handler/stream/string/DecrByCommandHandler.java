package io.github.dimkich.integration.testing.redis.replication.handler.stream.string;

import com.moilioncircle.redis.replicator.cmd.impl.DecrByCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class DecrByCommandHandler implements RedisStreamHandler<DecrByCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == DecrByCommand.class;
    }

    @Override
    public void handle(DecrByCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, entry) -> entry.increment(-event.getValue()));
    }
}

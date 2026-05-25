package io.github.dimkich.integration.testing.redis.replication.handler.stream.string;

import com.moilioncircle.redis.replicator.cmd.impl.DecrCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class DecrCommandHandler implements RedisStreamHandler<DecrCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == DecrCommand.class;
    }

    @Override
    public void handle(DecrCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, entry) -> entry.increment(-1));
    }
}

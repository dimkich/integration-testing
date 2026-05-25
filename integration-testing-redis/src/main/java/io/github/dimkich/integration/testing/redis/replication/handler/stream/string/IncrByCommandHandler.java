package io.github.dimkich.integration.testing.redis.replication.handler.stream.string;

import com.moilioncircle.redis.replicator.cmd.impl.IncrByCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class IncrByCommandHandler implements RedisStreamHandler<IncrByCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> cls) {
        return cls == IncrByCommand.class;
    }

    @Override
    public void handle(IncrByCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, e) -> e.increment(event.getValue()));
    }
}
package io.github.dimkich.integration.testing.redis.replication.handler.stream.string;

import com.moilioncircle.redis.replicator.cmd.impl.GetSetCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class GetSetCommandHandler implements RedisStreamHandler<GetSetCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == GetSetCommand.class;
    }

    @Override
    public void handle(GetSetCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, entry) -> entry.setValue(schema, event.getValue()));
    }
}

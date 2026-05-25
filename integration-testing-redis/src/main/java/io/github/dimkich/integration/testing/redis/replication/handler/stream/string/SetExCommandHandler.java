package io.github.dimkich.integration.testing.redis.replication.handler.stream.string;

import com.moilioncircle.redis.replicator.cmd.impl.SetExCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class SetExCommandHandler implements RedisStreamHandler<SetExCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == SetExCommand.class;
    }

    @Override
    public void handle(SetExCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, entry) -> {
            entry.setValue(schema, event.getValue());
            entry.setExpireAt(store.getNow(), store.getNow().plusSeconds(event.getEx()));
        });
    }
}
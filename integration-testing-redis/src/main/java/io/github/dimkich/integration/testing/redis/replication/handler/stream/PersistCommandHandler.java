package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.PersistCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
public class PersistCommandHandler implements RedisStreamHandler<PersistCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == PersistCommand.class;
    }

    @Override
    public void handle(PersistCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, entry) -> entry.setExpireAt(store.getNow(), (ZonedDateTime) null));
    }
}
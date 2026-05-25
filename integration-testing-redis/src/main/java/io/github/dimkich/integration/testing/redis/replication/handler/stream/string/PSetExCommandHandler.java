package io.github.dimkich.integration.testing.redis.replication.handler.stream.string;

import com.moilioncircle.redis.replicator.cmd.impl.PSetExCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;

@Component
public class PSetExCommandHandler implements RedisStreamHandler<PSetExCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == PSetExCommand.class;
    }

    @Override
    public void handle(PSetExCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, entry) -> {
            entry.setValue(schema, event.getValue());
            entry.setExpireAt(store.getNow(), store.getNow().plus(event.getEx(), ChronoUnit.MILLIS));
        });
    }
}

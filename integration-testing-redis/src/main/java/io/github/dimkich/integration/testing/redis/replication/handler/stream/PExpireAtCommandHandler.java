package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.PExpireAtCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;

@Component
public class PExpireAtCommandHandler implements RedisStreamHandler<PExpireAtCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == PExpireAtCommand.class;
    }

    @Override
    public void handle(PExpireAtCommand command, RedisInMemoryStore store) {
        store.compute(command.getKey(), (schema, entry) -> {
            if (entry.getData() == null) {
                return;
            }
            ZonedDateTime now = store.getNow();
            ZonedDateTime newExpireAt = Instant.ofEpochMilli(command.getEx()).atZone(now.getZone());
            entry.setExpireAt(now, newExpireAt);
        });
    }
}
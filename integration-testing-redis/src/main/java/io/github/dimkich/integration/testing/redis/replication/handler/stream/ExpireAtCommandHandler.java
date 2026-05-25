package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.ExpireAtCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;

@Component
public class ExpireAtCommandHandler implements RedisStreamHandler<ExpireAtCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == ExpireAtCommand.class;
    }

    @Override
    public void handle(ExpireAtCommand command, RedisInMemoryStore store) {
        store.compute(command.getKey(), (schema, entry) -> {
            if (entry.getData() == null) {
                return;
            }
            ZonedDateTime now = store.getNow();
            ZonedDateTime newExpireAt = Instant.ofEpochSecond(command.getEx()).atZone(now.getZone());
            entry.setExpireAt(now, newExpireAt);
        });
    }
}
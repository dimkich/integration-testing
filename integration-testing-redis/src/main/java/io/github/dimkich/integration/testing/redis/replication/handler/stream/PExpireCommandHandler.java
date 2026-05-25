package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.PExpireCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class PExpireCommandHandler implements RedisStreamHandler<PExpireCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == PExpireCommand.class;
    }

    @Override
    public void handle(PExpireCommand command, RedisInMemoryStore store) {
        store.compute(command.getKey(), (schema, entry) -> {
            if (entry.getData() == null) {
                return;
            }
            ZonedDateTime now = store.getNow();
            ZonedDateTime newExpireAt = now.plus(command.getEx(), ChronoUnit.MILLIS);
            entry.setExpireAt(now, newExpireAt);
        });
    }
}
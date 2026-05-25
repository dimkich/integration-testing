package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.ExpireCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class ExpireCommandHandler implements RedisStreamHandler<ExpireCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == ExpireCommand.class;
    }

    @Override
    public void handle(ExpireCommand command, RedisInMemoryStore store) {
        store.compute(command.getKey(), (schema, entry) -> {
            if (entry.getData() == null) {
                return;
            }
            ZonedDateTime now = store.getNow();
            ZonedDateTime newExpireAt = now.truncatedTo(ChronoUnit.SECONDS).plusSeconds(command.getEx());
            entry.setExpireAt(now, newExpireAt);
        });
    }
}
package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.MoveCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
public class MoveCommandHandler implements RedisStreamHandler<MoveCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == MoveCommand.class;
    }

    @Override
    public void handle(MoveCommand event, RedisInMemoryStore store) {
        byte[] keyRaw = event.getKey();
        if (store.exists(keyRaw)) {
            store.compute(event.getKey(), (schema, entry) -> {
                if (entry.getData() != null) {
                    Object data = entry.getData();
                    ZonedDateTime expireAt = entry.getExpireAt();
                    store.remove(keyRaw);
                    store.compute(event.getDb(), keyRaw, (targetSchema, targetEntry) -> {
                        targetEntry.setData(data);
                        targetEntry.setExpireAt(store.getNow(), expireAt);
                    });
                }
            });
        }
    }
}
package io.github.dimkich.integration.testing.redis.replication.handler.stream.hash;

import com.moilioncircle.redis.replicator.cmd.impl.HPExpireAtCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisEntry;
import io.github.dimkich.integration.testing.redis.model.RedisHash;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
public class HPExpireAtCommandHandler implements RedisStreamHandler<HPExpireAtCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == HPExpireAtCommand.class;
    }

    @Override
    public void handle(HPExpireAtCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisHash.class, (schema, hash) -> {
            ZonedDateTime now = store.getNow();
            for (byte[] fieldRaw : event.getFields()) {
                Object fieldKey = schema.getHashKeyCodec().deserialize(fieldRaw);
                RedisEntry entry = hash.get(fieldKey);
                if (entry != null) {
                    entry.setExpireAt(now, event.getEx());
                }
            }
        });
    }
}

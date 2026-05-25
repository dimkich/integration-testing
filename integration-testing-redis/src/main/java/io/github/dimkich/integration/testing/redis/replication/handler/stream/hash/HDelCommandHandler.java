package io.github.dimkich.integration.testing.redis.replication.handler.stream.hash;

import com.moilioncircle.redis.replicator.cmd.impl.HDelCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisHash;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class HDelCommandHandler implements RedisStreamHandler<HDelCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == HDelCommand.class;
    }

    @Override
    public void handle(HDelCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisHash.class, (schema, hash) -> {
            for (byte[] f : event.getFields()) {
                hash.remove(schema.getHashKeyCodec().deserialize(f));
            }
        });
    }
}
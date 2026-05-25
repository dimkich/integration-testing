package io.github.dimkich.integration.testing.redis.replication.handler.stream.hash;

import com.moilioncircle.redis.replicator.cmd.impl.HSetNxCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisHash;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class HSetNxCommandHandler implements RedisStreamHandler<HSetNxCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == HSetNxCommand.class;
    }

    @Override
    public void handle(HSetNxCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisHash.class, (schema, hash) -> {
            Object field = schema.getHashKeyCodec().deserialize(event.getField());
            if (!hash.containsKey(field)) {
                hash.putMapEntry(field, schema.getHashValueCodec().deserialize(event.getValue()));
            }
        });
    }
}
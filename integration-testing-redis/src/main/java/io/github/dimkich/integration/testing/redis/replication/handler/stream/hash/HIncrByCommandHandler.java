package io.github.dimkich.integration.testing.redis.replication.handler.stream.hash;

import com.moilioncircle.redis.replicator.cmd.impl.HIncrByCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisHash;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class HIncrByCommandHandler implements RedisStreamHandler<HIncrByCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == HIncrByCommand.class;
    }

    @Override
    public void handle(HIncrByCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisHash.class, (schema, hash) -> {
            Object field = schema.getHashKeyCodec().deserialize(event.getField());
            hash.get(field).increment(event.getIncrement());
        });
    }
}
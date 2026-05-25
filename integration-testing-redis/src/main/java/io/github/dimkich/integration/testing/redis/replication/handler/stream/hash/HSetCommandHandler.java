package io.github.dimkich.integration.testing.redis.replication.handler.stream.hash;

import com.moilioncircle.redis.replicator.cmd.impl.HSetCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisHash;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class HSetCommandHandler implements RedisStreamHandler<HSetCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == HSetCommand.class;
    }

    @Override
    public void handle(HSetCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisHash.class, (schema, hash) ->
                event.getFields().forEach((f, v) -> hash.putMapEntry(
                        schema.getHashKeyCodec().deserialize(f),
                        schema.getHashValueCodec().deserialize(v)
                ))
        );
    }
}
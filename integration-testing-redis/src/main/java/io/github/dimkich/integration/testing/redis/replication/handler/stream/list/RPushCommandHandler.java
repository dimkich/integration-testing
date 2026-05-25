package io.github.dimkich.integration.testing.redis.replication.handler.stream.list;

import com.moilioncircle.redis.replicator.cmd.impl.RPushCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class RPushCommandHandler implements RedisStreamHandler<RPushCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == RPushCommand.class;
    }

    @Override
    public void handle(RPushCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisList.class, (schema, list) -> {
            for (byte[] value : event.getValues()) {
                list.add(schema.getValueCodec().deserialize(value));
            }
        });
    }
}
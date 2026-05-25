package io.github.dimkich.integration.testing.redis.replication.handler.stream.list;

import com.moilioncircle.redis.replicator.cmd.impl.RPushXCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class RPushXCommandHandler implements RedisStreamHandler<RPushXCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == RPushXCommand.class;
    }

    @Override
    public void handle(RPushXCommand event, RedisInMemoryStore store) {
        if (!store.exists(event.getKey())) {
            return;
        }
        store.compute(event.getKey(), RedisList.class, (schema, list) -> {
            for (byte[] value : event.getValues()) {
                list.add(schema.getValueCodec().deserialize(value));
            }
        });
    }
}
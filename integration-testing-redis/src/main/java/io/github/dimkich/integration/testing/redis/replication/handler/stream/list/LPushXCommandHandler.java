package io.github.dimkich.integration.testing.redis.replication.handler.stream.list;

import com.moilioncircle.redis.replicator.cmd.impl.LPushXCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class LPushXCommandHandler implements RedisStreamHandler<LPushXCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == LPushXCommand.class;
    }

    @Override
    public void handle(LPushXCommand event, RedisInMemoryStore store) {
        if (!store.exists(event.getKey())) {
            return;
        }
        store.compute(event.getKey(), RedisList.class, (schema, list) -> {
            for (byte[] value : event.getValues()) {
                list.add(0, schema.getValueCodec().deserialize(value));
            }
        });
    }
}
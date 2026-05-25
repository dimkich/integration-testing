package io.github.dimkich.integration.testing.redis.replication.handler.stream.list;

import com.moilioncircle.redis.replicator.cmd.impl.LSetCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class LSetCommandHandler implements RedisStreamHandler<LSetCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == LSetCommand.class;
    }

    @Override
    public void handle(LSetCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisList.class, (schema, list) -> {
            int index = (int) event.getIndex();
            if (index < 0) index = list.size() + index;
            if (index >= 0 && index < list.size()) {
                list.set(index, schema.getValueCodec().deserialize(event.getValue()));
            }
        });
    }
}
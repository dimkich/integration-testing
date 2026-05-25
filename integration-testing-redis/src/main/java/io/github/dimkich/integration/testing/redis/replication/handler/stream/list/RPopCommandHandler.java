package io.github.dimkich.integration.testing.redis.replication.handler.stream.list;

import com.moilioncircle.redis.replicator.cmd.impl.RPopCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class RPopCommandHandler implements RedisStreamHandler<RPopCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == RPopCommand.class;
    }

    @Override
    public void handle(RPopCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisList.class, (schema, list) -> {
            long count = (event.getCount() == null) ? 1 : event.getCount().getCount();
            for (int i = 0; i < count && !list.isEmpty(); i++) {
                list.remove(list.size() - 1);
            }
        });
    }
}
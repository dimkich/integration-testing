package io.github.dimkich.integration.testing.redis.replication.handler.stream.list;

import com.moilioncircle.redis.replicator.cmd.impl.RPopLPushCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class RPopLPushCommandHandler implements RedisStreamHandler<RPopLPushCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == RPopLPushCommand.class;
    }

    @Override
    public void handle(RPopLPushCommand event, RedisInMemoryStore store) {
        final Object[] container = new Object[1];
        store.compute(event.getSource(), RedisList.class, (schema, sourceList) -> {
            if (sourceList.isEmpty()) {
                return;
            }
            container[0] = sourceList.remove(sourceList.size() - 1);
        });
        if (container[0] != null) {
            store.compute(event.getDestination(), RedisList.class, (schema, destList) -> destList.add(0, container[0]));
        }
    }
}
package io.github.dimkich.integration.testing.redis.replication.handler.stream.list;

import com.moilioncircle.redis.replicator.cmd.impl.DirectionType;
import com.moilioncircle.redis.replicator.cmd.impl.LMoveCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class LMoveCommandHandler implements RedisStreamHandler<LMoveCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == LMoveCommand.class;
    }

    @Override
    public void handle(LMoveCommand event, RedisInMemoryStore store) {
        Object[] container = new Object[1];
        store.compute(event.getSource(), RedisList.class, (schema, sourceList) -> {
            if (sourceList.isEmpty()) {
                return;
            }
            int indexToRemove = (event.getFrom() == DirectionType.LEFT) ? 0 : sourceList.size() - 1;
            container[0] = sourceList.remove(indexToRemove);
        });
        if (container[0] != null) {
            store.compute(event.getDestination(), RedisList.class, (schema, destList) -> {
                if (event.getTo() == DirectionType.LEFT) {
                    destList.add(0, container[0]);
                } else {
                    destList.add(container[0]);
                }
            });
        }
    }
}
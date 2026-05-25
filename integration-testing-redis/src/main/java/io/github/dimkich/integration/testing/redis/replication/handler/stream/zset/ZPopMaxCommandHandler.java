package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.ZPopMaxCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class ZPopMaxCommandHandler implements RedisStreamHandler<ZPopMaxCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == ZPopMaxCommand.class;
    }

    @Override
    public void handle(ZPopMaxCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisZSet.class, (schema, zset) -> {
            int count = event.getCount();
            for (int i = 0; i < count && !zset.isEmpty(); i++) {
                zset.pollLast();
            }
        });
    }
}
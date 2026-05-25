package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.DelCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class DelCommandHandler implements RedisStreamHandler<DelCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return DelCommand.class == eventClass;
    }

    @Override
    public void handle(DelCommand event, RedisInMemoryStore store) {
        for (byte[] key : event.getKeys()) {
            store.remove(key);
        }
    }
}
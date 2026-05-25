package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.FlushDBCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class FlushDbCommandHandler implements RedisStreamHandler<FlushDBCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == FlushDBCommand.class;
    }

    @Override
    public void handle(FlushDBCommand event, RedisInMemoryStore store) {
        store.flushDb();
    }
}
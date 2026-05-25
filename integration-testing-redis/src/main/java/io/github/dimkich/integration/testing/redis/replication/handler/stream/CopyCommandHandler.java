package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.CopyCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class CopyCommandHandler implements RedisStreamHandler<CopyCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == CopyCommand.class;
    }

    @Override
    public void handle(CopyCommand event, RedisInMemoryStore store) {
        store.copy(event.getSource(), event.getDestination(), event.isReplace());
    }
}
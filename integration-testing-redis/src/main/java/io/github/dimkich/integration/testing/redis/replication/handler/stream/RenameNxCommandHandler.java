package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.RenameNxCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class RenameNxCommandHandler implements RedisStreamHandler<RenameNxCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == RenameNxCommand.class;
    }

    @Override
    public void handle(RenameNxCommand event, RedisInMemoryStore store) {
        store.rename(event.getKey(), event.getNewKey());
    }
}
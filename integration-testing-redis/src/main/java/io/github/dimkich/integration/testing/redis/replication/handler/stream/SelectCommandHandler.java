package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.SelectCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class SelectCommandHandler implements RedisStreamHandler<SelectCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return SelectCommand.class == eventClass;
    }

    @Override
    public void handle(SelectCommand event, RedisInMemoryStore store) {
        store.setCurrentDb(event.getIndex());
    }
}

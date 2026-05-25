package io.github.dimkich.integration.testing.redis.replication.handler.stream.hll;

import com.moilioncircle.redis.replicator.cmd.impl.PFCountCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class PFCountCommandHandler implements RedisStreamHandler<PFCountCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == PFCountCommand.class;
    }

    @Override
    public void handle(PFCountCommand event, RedisInMemoryStore store) {
    }
}
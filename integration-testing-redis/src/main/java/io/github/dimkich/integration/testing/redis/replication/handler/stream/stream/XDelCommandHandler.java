package io.github.dimkich.integration.testing.redis.replication.handler.stream.stream;

import com.moilioncircle.redis.replicator.cmd.impl.XDelCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class XDelCommandHandler implements RedisStreamHandler<XDelCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == XDelCommand.class;
    }

    @Override
    public void handle(XDelCommand event, RedisInMemoryStore store) {
        StreamEntries.removeByIds(store, event.getKey(), event.getIds());
    }
}

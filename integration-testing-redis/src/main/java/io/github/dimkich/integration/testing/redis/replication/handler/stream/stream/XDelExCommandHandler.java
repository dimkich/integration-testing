package io.github.dimkich.integration.testing.redis.replication.handler.stream.stream;

import com.moilioncircle.redis.replicator.cmd.impl.XDelExCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class XDelExCommandHandler implements RedisStreamHandler<XDelExCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == XDelExCommand.class;
    }

    @Override
    public void handle(XDelExCommand event, RedisInMemoryStore store) {
        StreamEntries.removeByIds(store, event.getKey(), event.getIds());
    }
}

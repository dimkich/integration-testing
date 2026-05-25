package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.UnLinkCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class UnlinkCommandHandler implements RedisStreamHandler<UnLinkCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == UnLinkCommand.class;
    }

    @Override
    public void handle(UnLinkCommand event, RedisInMemoryStore store) {
        for (byte[] key : event.getKeys()) {
            store.remove(key);
        }
    }
}
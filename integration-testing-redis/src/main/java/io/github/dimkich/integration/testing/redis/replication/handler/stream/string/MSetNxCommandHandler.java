package io.github.dimkich.integration.testing.redis.replication.handler.stream.string;

import com.moilioncircle.redis.replicator.cmd.impl.MSetNxCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class MSetNxCommandHandler implements RedisStreamHandler<MSetNxCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == MSetNxCommand.class;
    }

    @Override
    public void handle(MSetNxCommand event, RedisInMemoryStore store) {
        event.getKv().forEach((key, value) -> store.compute(key, (s, e) -> e.setValue(s, value)));
    }
}
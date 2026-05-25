package io.github.dimkich.integration.testing.redis.replication.handler.stream.stream;

import com.moilioncircle.redis.replicator.cmd.impl.XSetIdCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisStream;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class XSetIdCommandHandler implements RedisStreamHandler<XSetIdCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == XSetIdCommand.class;
    }

    @Override
    public void handle(XSetIdCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisStream.class, (schema, stream) -> {
        });
    }
}

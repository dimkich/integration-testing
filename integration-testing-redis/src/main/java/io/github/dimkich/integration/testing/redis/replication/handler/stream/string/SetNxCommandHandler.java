package io.github.dimkich.integration.testing.redis.replication.handler.stream.string;

import com.moilioncircle.redis.replicator.cmd.impl.SetNxCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class SetNxCommandHandler implements RedisStreamHandler<SetNxCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == SetNxCommand.class;
    }

    @Override
    public void handle(SetNxCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, e) -> {
            if (e.getData() == null) {
                e.setValue(schema, event.getValue());
            }
        });
    }
}
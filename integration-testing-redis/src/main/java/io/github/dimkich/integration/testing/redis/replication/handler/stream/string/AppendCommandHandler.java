package io.github.dimkich.integration.testing.redis.replication.handler.stream.string;

import com.moilioncircle.redis.replicator.cmd.impl.AppendCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class AppendCommandHandler implements RedisStreamHandler<AppendCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == AppendCommand.class;
    }

    @Override
    public void handle(AppendCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, entry) -> {
            String existing = entry.getData() == null ? "" : entry.getData().toString();
            String toAppend = (String) schema.getValueCodec().deserialize(event.getValue());
            entry.setData(existing + toAppend);
        });
    }
}

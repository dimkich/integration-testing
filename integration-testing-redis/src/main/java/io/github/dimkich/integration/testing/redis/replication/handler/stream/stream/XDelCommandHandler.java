package io.github.dimkich.integration.testing.redis.replication.handler.stream.stream;

import com.moilioncircle.redis.replicator.cmd.impl.XDelCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisStream;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class XDelCommandHandler implements RedisStreamHandler<XDelCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == XDelCommand.class;
    }

    @Override
    public void handle(XDelCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisStream.class, (schema, stream) -> {
            Set<String> idsToDelete = Arrays.stream(event.getIds())
                    .map(id -> new String(id, StandardCharsets.UTF_8))
                    .collect(Collectors.toSet());
            stream.removeIf(entry -> idsToDelete.contains(entry.getId()));
        });
    }
}

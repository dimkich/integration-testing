package io.github.dimkich.integration.testing.redis.replication.handler.stream.stream;

import com.moilioncircle.redis.replicator.cmd.impl.XTrimCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisStream;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class XTrimCommandHandler implements RedisStreamHandler<XTrimCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == XTrimCommand.class;
    }

    @Override
    public void handle(XTrimCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisStream.class, (schema, stream) -> {
            if (stream.isEmpty()) {
                return;
            }

            if (event.getMaxLen() != null) {
                int maxLen = (int) event.getMaxLen().getCount();
                if (stream.size() > maxLen) {
                    int toRemove = stream.size() - maxLen;
                    stream.subList(0, toRemove).clear();
                }
            } else if (event.getMinId() != null) {
                String minId = new String(event.getMinId().getId(), StandardCharsets.UTF_8);
                stream.removeIf(entry -> entry.getId().compareTo(minId) < 0);
            }
        });
    }
}

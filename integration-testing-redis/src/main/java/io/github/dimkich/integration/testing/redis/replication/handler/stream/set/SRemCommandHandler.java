package io.github.dimkich.integration.testing.redis.replication.handler.stream.set;

import com.moilioncircle.redis.replicator.cmd.impl.SRemCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class SRemCommandHandler implements RedisStreamHandler<SRemCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == SRemCommand.class;
    }

    @Override
    public void handle(SRemCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisSet.class, (schema, set) -> {
            for (byte[] memberRaw : event.getMembers()) {
                Object member = schema.getValueCodec().deserialize(memberRaw);
                set.remove(member);
            }
        });
    }
}
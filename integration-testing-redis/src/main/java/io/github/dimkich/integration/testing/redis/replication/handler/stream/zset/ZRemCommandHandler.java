package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.ZRemCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class ZRemCommandHandler implements RedisStreamHandler<ZRemCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == ZRemCommand.class;
    }

    @Override
    public void handle(ZRemCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisZSet.class, (schema, zset) -> {
            for (byte[] memberRaw : event.getMembers()) {
                Object member = schema.getValueCodec().deserialize(memberRaw);
                zset.removeMember(member);
            }
        });
    }
}
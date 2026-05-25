package io.github.dimkich.integration.testing.redis.replication.handler.stream.hll;

import com.moilioncircle.redis.replicator.cmd.impl.PFAddCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisHyperLogLog;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class PFAddCommandHandler implements RedisStreamHandler<PFAddCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == PFAddCommand.class;
    }

    @Override
    public void handle(PFAddCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisHyperLogLog.class, (schema, hll) -> {
            for (byte[] element : event.getElements()) {
                hll.add(schema.getValueCodec().deserialize(element));
            }
        });
    }
}
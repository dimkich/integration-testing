package io.github.dimkich.integration.testing.redis.replication.handler.stream.set;

import com.moilioncircle.redis.replicator.cmd.impl.SUnionStoreCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class SUnionStoreCommandHandler implements RedisStreamHandler<SUnionStoreCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == SUnionStoreCommand.class;
    }

    @Override
    public void handle(SUnionStoreCommand event, RedisInMemoryStore store) {
        Set<Object> unionResult = new HashSet<>();
        for (byte[] sourceKey : event.getKeys()) {
            store.compute(sourceKey, RedisSet.class, (schema, sourceSet) -> unionResult.addAll(sourceSet));
        }
        store.compute(event.getDestination(), RedisSet.class, (schema, destSet) -> {
            destSet.clear();
            destSet.addAll(unionResult);
        });
    }
}
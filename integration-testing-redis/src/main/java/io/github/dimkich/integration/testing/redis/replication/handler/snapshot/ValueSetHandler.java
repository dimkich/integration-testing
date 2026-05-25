package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueSet;
import io.github.dimkich.integration.testing.redis.model.RedisSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class ValueSetHandler implements RedisSnapshotHandler<KeyStringValueSet> {
    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == KeyStringValueSet.class;
    }

    @Override
    public void handle(KeyStringValueSet event, RedisInMemoryStore store) {
        store.compute(event, RedisSet.class, (s, set) -> event.getValue().forEach(o -> set.add(s, o)));
    }
}


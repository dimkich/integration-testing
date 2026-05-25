package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueZSet;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class ValueZSetHandler implements RedisSnapshotHandler<KeyStringValueZSet> {
    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == KeyStringValueZSet.class;
    }

    @Override
    public void handle(KeyStringValueZSet event, RedisInMemoryStore store) {
        store.compute(event, RedisZSet.class, (s, z) -> event.getValue().forEach(o -> z.putEntry(s, o)));
    }
}


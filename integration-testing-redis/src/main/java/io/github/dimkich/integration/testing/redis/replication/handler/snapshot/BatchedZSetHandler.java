package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyStringValueZSet;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class BatchedZSetHandler implements RedisSnapshotHandler<BatchedKeyStringValueZSet> {
    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == BatchedKeyStringValueZSet.class;
    }

    @Override
    public void handle(BatchedKeyStringValueZSet event, RedisInMemoryStore store) {
        store.compute(event, RedisZSet.class, (s, z) -> event.getValue().forEach(o -> z.putEntry(s, o)));
    }
}


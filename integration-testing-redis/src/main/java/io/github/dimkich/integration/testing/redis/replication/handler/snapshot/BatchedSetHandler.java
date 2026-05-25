package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyStringValueSet;
import io.github.dimkich.integration.testing.redis.model.RedisSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class BatchedSetHandler implements RedisSnapshotHandler<BatchedKeyStringValueSet> {
    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == BatchedKeyStringValueSet.class;
    }

    @Override
    public void handle(BatchedKeyStringValueSet event, RedisInMemoryStore store) {
        store.compute(event, RedisSet.class, (s, o) -> event.getValue().forEach(entry -> o.add(s, entry)));
    }
}
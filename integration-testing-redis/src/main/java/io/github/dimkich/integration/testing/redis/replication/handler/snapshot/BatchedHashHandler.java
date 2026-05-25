package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyStringValueHash;
import io.github.dimkich.integration.testing.redis.model.RedisHash;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class BatchedHashHandler implements RedisSnapshotHandler<BatchedKeyStringValueHash> {
    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == BatchedKeyStringValueHash.class;
    }

    @Override
    public void handle(BatchedKeyStringValueHash event, RedisInMemoryStore store) {
        store.compute(event, RedisHash.class, (s, o) ->
                event.getValue().entrySet().forEach(entry -> o.putMapEntry(s, entry)));
    }
}
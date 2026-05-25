package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyStringValueTTLHash;
import io.github.dimkich.integration.testing.redis.model.RedisHash;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class BatchedTTLHashHandler implements RedisSnapshotHandler<BatchedKeyStringValueTTLHash> {
    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == BatchedKeyStringValueTTLHash.class;
    }

    @Override
    public void handle(BatchedKeyStringValueTTLHash event, RedisInMemoryStore store) {
        store.compute(event, RedisHash.class, (s, h) ->
                event.getValue().entrySet().forEach(o -> h.putTtlMapEntry(store, s, o)));
    }
}


package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.KeyStringValueMapEntryIterator;
import io.github.dimkich.integration.testing.redis.model.RedisHash;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class IteratorHashHandler implements RedisSnapshotHandler<KeyStringValueMapEntryIterator> {
    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == KeyStringValueMapEntryIterator.class;
    }

    @Override
    public void handle(KeyStringValueMapEntryIterator event, RedisInMemoryStore store) {
        store.compute(event, RedisHash.class, (s, h) ->
                event.getValue().forEachRemaining(entry -> h.putMapEntry(s, entry)));
    }
}

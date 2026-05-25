package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.KeyStringValueZSetEntryIterator;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class IteratorZSetEntryHandler implements RedisSnapshotHandler<KeyStringValueZSetEntryIterator> {
    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == KeyStringValueZSetEntryIterator.class;
    }

    @Override
    public void handle(KeyStringValueZSetEntryIterator event, RedisInMemoryStore store) {
        store.compute(event, RedisZSet.class, (s, z) -> event.getValue().forEachRemaining(o -> z.putEntry(s, o)));
    }
}


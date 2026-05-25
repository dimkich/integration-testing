package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueHash;
import io.github.dimkich.integration.testing.redis.model.RedisHash;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class ValueHashHandler implements RedisSnapshotHandler<KeyStringValueHash> {
    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == KeyStringValueHash.class;
    }

    @Override
    public void handle(KeyStringValueHash event, RedisInMemoryStore store) {
        store.compute(event, RedisHash.class, (s, h) -> event.getValue().entrySet().forEach(o -> h.putMapEntry(s, o)));
    }
}


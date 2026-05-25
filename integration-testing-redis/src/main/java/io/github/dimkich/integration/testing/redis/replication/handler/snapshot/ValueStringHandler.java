package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueString;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class ValueStringHandler implements RedisSnapshotHandler<KeyStringValueString> {
    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == KeyStringValueString.class;
    }

    @Override
    public void handle(KeyStringValueString event, RedisInMemoryStore store) {
        store.compute(event, (s, o) -> o.setValue(s, event.getValue()));
    }
}
package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueStream;
import io.github.dimkich.integration.testing.redis.model.RedisStream;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class ValueStreamHandler implements RedisSnapshotHandler<KeyStringValueStream> {

    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == KeyStringValueStream.class;
    }

    @Override
    public void handle(KeyStringValueStream event, RedisInMemoryStore store) {
        store.compute(event, RedisStream.class, (s, o) ->
                event.getValue().getEntries().values().forEach(e -> o.addEntry(s, e)));
    }
}

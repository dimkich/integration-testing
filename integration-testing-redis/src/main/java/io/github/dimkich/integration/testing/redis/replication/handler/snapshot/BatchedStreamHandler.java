package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyStringValueStream;
import io.github.dimkich.integration.testing.redis.model.RedisStream;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class BatchedStreamHandler implements RedisSnapshotHandler<BatchedKeyStringValueStream> {

    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == BatchedKeyStringValueStream.class;
    }

    @Override
    public void handle(BatchedKeyStringValueStream event, RedisInMemoryStore store) {
        store.compute(event, RedisStream.class, (s, o) ->
                event.getValue().getEntries().values().forEach(se -> o.addEntry(s, se)));
    }
}

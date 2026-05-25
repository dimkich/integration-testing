package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyStringValueList;
import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class BatchedListHandler implements RedisSnapshotHandler<BatchedKeyStringValueList> {
    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == BatchedKeyStringValueList.class;
    }

    @Override
    public void handle(BatchedKeyStringValueList event, RedisInMemoryStore store) {
        store.compute(event, RedisList.class, (s, o) -> event.getValue().forEach(entry -> o.add(s, entry)));
    }
}
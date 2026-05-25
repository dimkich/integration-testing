package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueList;
import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class ValueListHandler implements RedisSnapshotHandler<KeyStringValueList> {
    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == KeyStringValueList.class;
    }

    @Override
    public void handle(KeyStringValueList event, RedisInMemoryStore store) {
        store.compute(event, RedisList.class, (s, l) -> event.getValue().forEach(o -> l.add(s, o)));
    }
}


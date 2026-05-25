package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.Constants;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.KeyStringValueByteArrayIterator;
import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.model.RedisSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

@Component
public class IteratorListSetHandler implements RedisSnapshotHandler<KeyStringValueByteArrayIterator> {

    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == KeyStringValueByteArrayIterator.class;
    }

    @Override
    public void handle(KeyStringValueByteArrayIterator event, RedisInMemoryStore store) {
        if (event.getValueRdbType() == Constants.RDB_TYPE_LIST
                || event.getValueRdbType() == Constants.RDB_TYPE_LIST_ZIPLIST
                || event.getValueRdbType() == Constants.RDB_TYPE_LIST_QUICKLIST
                || event.getValueRdbType() == Constants.RDB_TYPE_LIST_QUICKLIST_2) {
            store.compute(event, RedisList.class, (s, l) -> event.getValue().forEachRemaining(e -> l.add(s, e)));
        } else {
            store.compute(event, RedisSet.class, (s, l) -> event.getValue().forEachRemaining(e -> l.add(s, e)));
        }
    }
}


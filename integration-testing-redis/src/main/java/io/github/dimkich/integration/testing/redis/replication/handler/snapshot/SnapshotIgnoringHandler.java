package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.cmd.TimestampEvent;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.AuxField;
import com.moilioncircle.redis.replicator.rdb.datatype.ContextKeyValuePair;
import com.moilioncircle.redis.replicator.rdb.datatype.Function;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import com.moilioncircle.redis.replicator.rdb.dump.datatype.DumpFunction;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyValuePair;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SnapshotIgnoringHandler implements RedisSnapshotHandler<Event> {
    private static final Set<Class<? extends Event>> IGNORED = Set.of(DumpFunction.class, AuxField.class,
            Function.class, ContextKeyValuePair.class, KeyValuePair.class, BatchedKeyValuePair.class,
            TimestampEvent.class);

    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return IGNORED.contains(clazz);
    }

    @Override
    public void handle(Event event, RedisInMemoryStore store) {
    }
}

package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.RestoreCommand;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.DB;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import com.moilioncircle.redis.replicator.rdb.dump.datatype.DumpKeyValuePair;
import com.moilioncircle.redis.replicator.rdb.dump.parser.DefaultDumpValueParser;
import io.github.dimkich.integration.testing.redis.replication.RedisEventDispatcher;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class RestoreCommandHandler implements RedisStreamHandler<RestoreCommand> {
    private final RedisEventDispatcher snapshotDispatcher;

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == RestoreCommand.class;
    }

    @Override
    public void handle(RestoreCommand cmd, RedisInMemoryStore store) {
        byte[] keyRaw = cmd.getKey();
        long ttl = cmd.getTtl();
        byte[] serializedValue = cmd.getSerializedValue();
        boolean replace = cmd.isReplace();
        boolean absTtl = cmd.isAbsTtl();

        if (!replace && store.exists(keyRaw)) {
            return;
        }

        DumpKeyValuePair kv = new DumpKeyValuePair();
        kv.setKey(keyRaw);
        kv.setValue(serializedValue);
        kv.setDb(new DB(store.getCurrentDb()));

        DefaultDumpValueParser parser = new DefaultDumpValueParser(store.getReplicator());
        KeyValuePair<?, ?> event = parser.parse(kv);

        store.remove(keyRaw);
        snapshotDispatcher.handle(event, store);

        if (ttl > 0) {
            store.compute(store.getCurrentDb(), keyRaw, (schema, entry) -> {
                ZonedDateTime expireAt;
                if (absTtl) {
                    expireAt = Instant.ofEpochMilli(ttl).atZone(store.getNow().getZone());
                } else {
                    expireAt = store.getNow().plus(ttl, ChronoUnit.MILLIS);
                }
                entry.setExpireAt(store.getNow(), expireAt);
            });
        } else if (ttl == 0) {
            store.compute(store.getCurrentDb(), keyRaw, (schema, entry) -> entry.setExpireAt(store.getNow(), (ZonedDateTime) null));
        }
    }
}
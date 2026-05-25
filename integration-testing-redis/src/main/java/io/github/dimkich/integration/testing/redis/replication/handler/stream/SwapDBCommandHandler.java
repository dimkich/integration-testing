package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.SwapDBCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisEntry;
import io.github.dimkich.integration.testing.redis.model.RedisKey;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SwapDBCommandHandler implements RedisStreamHandler<SwapDBCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == SwapDBCommand.class;
    }

    @Override
    public void handle(SwapDBCommand event, RedisInMemoryStore store) {
        int db1 = event.getSource();
        int db2 = event.getTarget();

        if (db1 == db2) {
            return;
        }

        store.getCurrentValue().entrySet().stream()
                .filter(e -> Objects.equals(e.getKey().getDb(), db1)
                        || Objects.equals(e.getKey().getDb(), db2))
                .toList()
                .forEach(entry -> {
                    RedisKey key = entry.getKey();
                    RedisEntry value = entry.getValue();
                    store.removeKey(key);
                    RedisKey newKey = new RedisKey();
                    newKey.setKey(key.getKey());
                    newKey.setDb(Objects.equals(key.getDb(), db1) ? db2 : db1);
                    newKey.setIgnored(key.isIgnored());
                    store.putKey(newKey, value);
                });
    }
}
package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.ZDiffStoreCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.model.RedisZSetEntry;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ZDiffStoreCommandHandler implements RedisStreamHandler<ZDiffStoreCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == ZDiffStoreCommand.class;
    }

    @Override
    public void handle(ZDiffStoreCommand event, RedisInMemoryStore store) {
        Map<Object, BigDecimal> diffMap = new LinkedHashMap<>();
        byte[][] sourceKeys = event.getKeys();

        if (sourceKeys == null || sourceKeys.length == 0) {
            return;
        }

        store.compute(sourceKeys[0], RedisZSet.class, (schema, firstZSet) -> {
            for (RedisZSetEntry entry : firstZSet) {
                diffMap.put(entry.getMember(), entry.getScore());
            }
        });

        for (int i = 1; i < sourceKeys.length; i++) {
            if (diffMap.isEmpty()) {
                break;
            }
            byte[] currentKey = sourceKeys[i];
            store.compute(currentKey, RedisZSet.class, (schema, otherZSet) -> {
                for (RedisZSetEntry entry : otherZSet) {
                    diffMap.remove(entry.getMember());
                }
            });
        }

        if (diffMap.isEmpty()) {
            store.remove(event.getDestination());
        } else {
            store.compute(event.getDestination(), RedisZSet.class, (schema, destZSet) -> {
                destZSet.clear();
                diffMap.forEach(destZSet::put);
            });
        }
    }
}
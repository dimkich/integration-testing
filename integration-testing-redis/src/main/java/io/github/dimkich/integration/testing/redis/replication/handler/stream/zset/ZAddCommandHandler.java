package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.CompareType;
import com.moilioncircle.redis.replicator.cmd.impl.ExistType;
import com.moilioncircle.redis.replicator.cmd.impl.ZAddCommand;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.ZSetEntry;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ZAddCommandHandler implements RedisStreamHandler<ZAddCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == ZAddCommand.class;
    }

    @Override
    public void handle(ZAddCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisZSet.class, (schema, zset) -> {
            ExistType existType = event.getExistType();
            CompareType compareType = event.getCompareType();

            for (ZSetEntry entry : event.getZSetEntries()) {
                Object member = schema.getValueCodec().deserialize(entry.getElement());
                BigDecimal newScore = BigDecimal.valueOf(entry.getScore());
                BigDecimal currentScore = zset.getScore(member);
                boolean exists = (currentScore != null);
                if (existType == ExistType.NX && exists) {
                    continue;
                }
                if (existType == ExistType.XX && !exists) {
                    continue;
                }

                if (exists && compareType != null && compareType != CompareType.NONE) {
                    int cmp = newScore.compareTo(currentScore);
                    if (compareType == CompareType.GT && cmp <= 0) {
                        continue;
                    }
                    if (compareType == CompareType.LT && cmp >= 0) {
                        continue;
                    }
                }
                zset.put(member, newScore);
            }
        });
    }
}
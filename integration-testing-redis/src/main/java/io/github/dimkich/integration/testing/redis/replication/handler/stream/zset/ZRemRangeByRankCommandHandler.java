package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.ZRemRangeByRankCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.model.RedisZSetEntry;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@Component
public class ZRemRangeByRankCommandHandler implements RedisStreamHandler<ZRemRangeByRankCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == ZRemRangeByRankCommand.class;
    }

    @Override
    public void handle(ZRemRangeByRankCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisZSet.class, (schema, zset) -> {
            int size = zset.size();
            if (size == 0) {
                return;
            }

            int start = (int) event.getStart();
            int stop = (int) event.getStop();
            if (start < 0) {
                start = size + start;
            }
            if (stop < 0) {
                stop = size + stop;
            }
            if (start < 0) {
                start = 0;
            }
            if (start >= size || start > stop) {
                return;
            }
            if (stop >= size) {
                stop = size - 1;
            }

            Iterator<RedisZSetEntry> it = zset.iterator();
            int currentRank = 0;
            while (it.hasNext()) {
                it.next();
                if (currentRank >= start) {
                    it.remove();
                }
                currentRank++;
                if (currentRank > stop) {
                    break;
                }
            }
        });
    }
}
package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.ZRemRangeByRankCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.model.RedisZSetEntry;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import io.github.dimkich.integration.testing.redis.replication.handler.stream.RankRange;
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
            RankRange range = RankRange.of(zset.size(), event.getStart(), event.getStop());
            if (range == null) {
                return;
            }

            Iterator<RedisZSetEntry> iterator = zset.iterator();
            for (int rank = 0; rank <= range.getStop() && iterator.hasNext(); rank++) {
                iterator.next();
                if (rank >= range.getStart()) {
                    iterator.remove();
                }
            }
        });
    }
}
package io.github.dimkich.integration.testing.redis.replication.handler.stream.list;

import com.moilioncircle.redis.replicator.cmd.impl.LTrimCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import io.github.dimkich.integration.testing.redis.replication.handler.stream.RankRange;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LTrimCommandHandler implements RedisStreamHandler<LTrimCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == LTrimCommand.class;
    }

    @Override
    public void handle(LTrimCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisList.class, (schema, list) -> {
            RankRange range = RankRange.of(list.size(), event.getStart(), event.getStop());
            if (range == null) {
                list.clear();
                return;
            }

            List<Object> kept = new ArrayList<>(list.subList(range.getStart(), range.getStop() + 1));
            list.clear();
            list.addAll(kept);
        });
    }
}
package io.github.dimkich.integration.testing.redis.replication.handler.stream.list;

import com.moilioncircle.redis.replicator.cmd.impl.LTrimCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
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
            int size = list.size();
            if (size == 0) return;

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
                list.clear();
                return;
            }
            if (stop >= size) {
                stop = size - 1;
            }

            List<Object> sub = new ArrayList<>(list.subList(start, stop + 1));
            list.clear();
            list.addAll(sub);
        });
    }
}
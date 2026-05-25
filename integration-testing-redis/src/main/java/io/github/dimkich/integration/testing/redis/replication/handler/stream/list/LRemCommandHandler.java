package io.github.dimkich.integration.testing.redis.replication.handler.stream.list;

import com.moilioncircle.redis.replicator.cmd.impl.LRemCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class LRemCommandHandler implements RedisStreamHandler<LRemCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == LRemCommand.class;
    }

    @Override
    public void handle(LRemCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisList.class, (schema, list) -> {
            Object valueToRemove = schema.getValueCodec().deserialize(event.getValue());
            long count = event.getIndex();

            if (count > 0) {
                for (int i = 0; i < list.size() && count > 0; i++) {
                    if (list.get(i).equals(valueToRemove)) {
                        list.remove(i--);
                        count--;
                    }
                }
            } else if (count < 0) {
                count = Math.abs(count);
                for (int i = list.size() - 1; i >= 0 && count > 0; i--) {
                    if (list.get(i).equals(valueToRemove)) {
                        list.remove(i);
                        count--;
                    }
                }
            } else {
                list.removeIf(v -> v.equals(valueToRemove));
            }
        });
    }
}
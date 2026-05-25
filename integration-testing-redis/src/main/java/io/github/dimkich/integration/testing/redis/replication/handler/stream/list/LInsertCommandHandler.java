package io.github.dimkich.integration.testing.redis.replication.handler.stream.list;

import com.moilioncircle.redis.replicator.cmd.impl.LInsertCommand;
import com.moilioncircle.redis.replicator.cmd.impl.LInsertType;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class LInsertCommandHandler implements RedisStreamHandler<LInsertCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == LInsertCommand.class;
    }

    @Override
    public void handle(LInsertCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisList.class, (schema, list) -> {
            Object pivot = schema.getValueCodec().deserialize(event.getPivot());
            Object value = schema.getValueCodec().deserialize(event.getValue());

            int index = list.indexOf(pivot);
            if (index != -1) {
                if (event.getlInsertType() == LInsertType.BEFORE) {
                    list.add(index, value);
                } else {
                    list.add(index + 1, value);
                }
            }
        });
    }
}
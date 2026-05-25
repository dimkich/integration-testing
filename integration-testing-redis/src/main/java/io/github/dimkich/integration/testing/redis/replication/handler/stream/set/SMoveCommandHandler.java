package io.github.dimkich.integration.testing.redis.replication.handler.stream.set;

import com.moilioncircle.redis.replicator.cmd.impl.SMoveCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class SMoveCommandHandler implements RedisStreamHandler<SMoveCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == SMoveCommand.class;
    }

    @Override
    public void handle(SMoveCommand event, RedisInMemoryStore store) {
        Object[] memberHolder = new Object[1];
        store.compute(event.getSource(), RedisSet.class, (schema, sourceSet) -> {
            Object member = schema.getValueCodec().deserialize(event.getMember());
            if (sourceSet.remove(member)) {
                memberHolder[0] = member;
            }
        });
        if (memberHolder[0] != null) {
            store.compute(event.getDestination(), RedisSet.class, (schema, destSet) -> destSet.add(memberHolder[0]));
        }
    }
}
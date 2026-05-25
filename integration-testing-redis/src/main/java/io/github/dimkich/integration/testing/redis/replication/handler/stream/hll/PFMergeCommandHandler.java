package io.github.dimkich.integration.testing.redis.replication.handler.stream.hll;

import com.moilioncircle.redis.replicator.cmd.impl.PFMergeCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisHyperLogLog;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class PFMergeCommandHandler implements RedisStreamHandler<PFMergeCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == PFMergeCommand.class;
    }

    @Override
    public void handle(PFMergeCommand event, RedisInMemoryStore store) {
        Set<Object> unionSet = new HashSet<>();

        store.compute(event.getDestkey(), RedisHyperLogLog.class, (schema, destHll) -> unionSet.addAll(destHll));

        for (byte[] sourceKeyRaw : event.getSourcekeys()) {
            store.compute(sourceKeyRaw, RedisHyperLogLog.class, (schema, sourceHll) -> unionSet.addAll(sourceHll));
        }

        store.compute(event.getDestkey(), RedisHyperLogLog.class, (schema, destHll) -> {
            destHll.clear();
            destHll.addAll(unionSet);
        });
    }
}
package io.github.dimkich.integration.testing.redis.replication.handler.stream.set;

import com.moilioncircle.redis.replicator.cmd.impl.SDiffStoreCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class SDiffStoreCommandHandler implements RedisStreamHandler<SDiffStoreCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == SDiffStoreCommand.class;
    }

    @Override
    public void handle(SDiffStoreCommand event, RedisInMemoryStore store) {
        byte[][] keys = event.getKeys();
        if (keys == null || keys.length == 0) {
            return;
        }

        Set<Object> diffResult = new HashSet<>();
        boolean[] firstKeyProcessed = {false};

        for (byte[] sourceKey : keys) {
            store.compute(sourceKey, RedisSet.class, (schema, sourceSet) -> {
                if (!firstKeyProcessed[0]) {
                    diffResult.addAll(sourceSet);
                    firstKeyProcessed[0] = true;
                } else {
                    diffResult.removeAll(sourceSet);
                }
            });
            if (firstKeyProcessed[0] && diffResult.isEmpty()) {
                break;
            }
        }

        if (diffResult.isEmpty()) {
            store.remove(event.getDestination());
        } else {
            store.compute(event.getDestination(), RedisSet.class, (schema, destSet) -> {
                destSet.clear();
                destSet.addAll(diffResult);
            });
        }
    }
}
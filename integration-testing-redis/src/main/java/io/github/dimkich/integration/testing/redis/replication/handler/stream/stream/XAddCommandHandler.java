package io.github.dimkich.integration.testing.redis.replication.handler.stream.stream;

import com.moilioncircle.redis.replicator.cmd.impl.XAddCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisStream;
import io.github.dimkich.integration.testing.redis.model.RedisStreamEntry;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class XAddCommandHandler implements RedisStreamHandler<XAddCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == XAddCommand.class;
    }

    @Override
    public void handle(XAddCommand event, RedisInMemoryStore store) {
        if (event.isNomkstream() && !store.exists(event.getKey())) {
            return;
        }

        store.compute(event.getKey(), RedisStream.class, (schema, stream) -> {
            String stringId = new String(event.getId(), StandardCharsets.UTF_8);

            Map<Object, Object> decodedFields = new LinkedHashMap<>();
            if (event.getFields() != null) {
                event.getFields().forEach((f, v) -> {
                    Object field = schema.getHashKeyCodec().deserialize(f);
                    Object value = schema.getHashValueCodec().deserialize(v);
                    decodedFields.put(field, value);
                });
            }
            stream.add(new RedisStreamEntry(stringId, decodedFields));
        });
    }
}

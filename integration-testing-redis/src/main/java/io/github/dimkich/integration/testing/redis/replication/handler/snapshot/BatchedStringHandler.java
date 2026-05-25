package io.github.dimkich.integration.testing.redis.replication.handler.snapshot;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyStringValueString;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class BatchedStringHandler implements RedisSnapshotHandler<BatchedKeyStringValueString> {
    private final Map<String, ByteArrayOutputStream> buffers = new HashMap<>();

    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == BatchedKeyStringValueString.class;
    }

    @Override
    @SneakyThrows
    public void handle(BatchedKeyStringValueString event, RedisInMemoryStore store) {
        ByteArrayOutputStream buf = buffers.computeIfAbsent(store.getName(),
                k -> new ByteArrayOutputStream(1024 * 128));
        if (event.getValue() != null) {
            buf.write(event.getValue());
        }
        if (event.isLast()) {
            byte[] result = buf.toByteArray();
            buf.reset();
            store.compute(event, (schema, entry) -> entry.setValue(schema, result));
        }
    }
}
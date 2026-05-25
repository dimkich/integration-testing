package io.github.dimkich.integration.testing.redis.replication.handler.stream.string;

import com.moilioncircle.redis.replicator.cmd.impl.SetRangeCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class SetRangeCommandHandler implements RedisStreamHandler<SetRangeCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == SetRangeCommand.class;
    }

    @Override
    public void handle(SetRangeCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, entry) -> {
            byte[] currentBytes = schema.getValueCodec().serialize(entry.getData());
            if (currentBytes == null) {
                currentBytes = new byte[0];
            }

            int offset = (int) event.getIndex();
            byte[] valueToOverwrite = event.getValue();
            int newLen = Math.max(currentBytes.length, offset + valueToOverwrite.length);
            byte[] result = Arrays.copyOf(currentBytes, newLen);
            System.arraycopy(valueToOverwrite, 0, result, offset, valueToOverwrite.length);
            entry.setData(schema.getValueCodec().deserialize(result));
        });
    }
}
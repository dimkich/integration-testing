package io.github.dimkich.integration.testing.redis.replication.handler.stream.string;

import com.moilioncircle.redis.replicator.cmd.impl.SetBitCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class SetBitCommandHandler implements RedisStreamHandler<SetBitCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == SetBitCommand.class;
    }

    @Override
    public void handle(SetBitCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, entry) -> {
            byte[] bytes;
            Object currentData = entry.getData();

            if (currentData instanceof byte[]) {
                bytes = (byte[]) currentData;
            } else if (currentData != null) {
                bytes = schema.getValueCodec().serialize(currentData);
            } else {
                bytes = new byte[0];
            }

            int offset = (int) event.getOffset();
            int byteIndex = offset / 8;
            int bitIndex = 7 - (offset % 8);

            if (byteIndex >= bytes.length) {
                bytes = Arrays.copyOf(bytes, byteIndex + 1);
            }

            if (event.getValue() == 1) {
                bytes[byteIndex] |= (byte) (1 << bitIndex);
            } else {
                bytes[byteIndex] &= (byte) ~(1 << bitIndex);
            }
            entry.setData(bytes);
        });
    }
}
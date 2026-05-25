package io.github.dimkich.integration.testing.redis.replication.handler.stream.string;

import com.moilioncircle.redis.replicator.cmd.impl.BitOpCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BitOpCommandHandler implements RedisStreamHandler<BitOpCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == BitOpCommand.class;
    }

    @Override
    public void handle(BitOpCommand event, RedisInMemoryStore store) {
        List<byte[]> sourceArrays = new ArrayList<>();
        int maxLength = 0;

        for (byte[] key : event.getKeys()) {
            final byte[][] holder = new byte[1][];
            store.compute(key, (schema, entry) -> {
                Object data = entry.getData();
                if (data instanceof byte[]) {
                    holder[0] = (byte[]) data;
                } else if (data != null) {
                    holder[0] = schema.getValueCodec().serialize(data);
                }
            });

            if (holder[0] != null) {
                sourceArrays.add(holder[0]);
                maxLength = Math.max(maxLength, holder[0].length);
            } else {
                sourceArrays.add(new byte[0]);
            }
        }

        String op = event.getOp().name().toUpperCase();
        if ("NOT".equals(op) && !sourceArrays.isEmpty()) {
            maxLength = sourceArrays.get(0).length;
        }

        if (maxLength == 0) {
            store.remove(event.getDestkey());
            return;
        }

        byte[] result = new byte[maxLength];

        for (int i = 0; i < maxLength; i++) {
            if ("NOT".equals(op)) {
                result[i] = (byte) ~(sourceArrays.get(0)[i]);
            } else {
                byte res = (i < sourceArrays.get(0).length) ? sourceArrays.get(0)[i] : 0;

                for (int j = 1; j < sourceArrays.size(); j++) {
                    byte b = (i < sourceArrays.get(j).length) ? sourceArrays.get(j)[i] : 0;
                    switch (op) {
                        case "AND" -> res &= b;
                        case "OR" -> res |= b;
                        case "XOR" -> res ^= b;
                    }
                }
                result[i] = res;
            }
        }

        store.compute(event.getDestkey(), (schema, entry) -> entry.setData(result));
    }
}
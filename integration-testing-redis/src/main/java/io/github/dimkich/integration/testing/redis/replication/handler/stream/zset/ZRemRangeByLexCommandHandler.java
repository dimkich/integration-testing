package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.ZRemRangeByLexCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class ZRemRangeByLexCommandHandler implements RedisStreamHandler<ZRemRangeByLexCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == ZRemRangeByLexCommand.class;
    }

    @Override
    public void handle(ZRemRangeByLexCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisZSet.class, (schema, zset) -> {
            if (zset.isEmpty()) {
                return;
            }
            String minStr = new String(event.getMin(), StandardCharsets.UTF_8);
            String maxStr = new String(event.getMax(), StandardCharsets.UTF_8);
            Range<String> range = parseLexRange(minStr, maxStr);
            zset.removeIf(entry -> range.contains(entry.getMember().toString()));
        });
    }

    private Range<String> parseLexRange(String min, String max) {
        return Range.of(parseBound(min, true), parseBound(max, false));
    }

    private Range.Bound<String> parseBound(String str, boolean isLower) {
        if (isLower && "-".equals(str)) {
            return Range.Bound.unbounded();
        }
        if (!isLower && "+".equals(str)) {
            return Range.Bound.unbounded();
        }
        if (str.startsWith("[")) {
            return Range.Bound.inclusive(str.substring(1));
        }
        if (str.startsWith("(")) {
            return Range.Bound.exclusive(str.substring(1));
        }
        return Range.Bound.inclusive(str);
    }
}
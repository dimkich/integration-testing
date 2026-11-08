package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.ZRemRangeByLexCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
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
            String min = new String(event.getMin(), StandardCharsets.UTF_8);
            String max = new String(event.getMax(), StandardCharsets.UTF_8);
            zset.removeIf(entry -> inRange(entry.getMember().toString(), min, max));
        });
    }

    private boolean inRange(String value, String min, String max) {
        if (!"-".equals(min) && !matchesLower(value, min)) {
            return false;
        }
        return "+".equals(max) || matchesUpper(value, max);
    }

    private boolean matchesLower(String value, String bound) {
        return matchesBound(value, bound, true);
    }

    private boolean matchesUpper(String value, String bound) {
        return matchesBound(value, bound, false);
    }

    /**
     * Compares {@code value} with a Redis lex bound ({@code [x}, {@code (x} or a bare value,
     * which Redis clients treat as inclusive).
     */
    private boolean matchesBound(String value, String bound, boolean lower) {
        boolean explicit = bound.startsWith("[") || bound.startsWith("(");
        boolean exclusive = bound.startsWith("(");
        int comparison = value.compareTo(explicit ? bound.substring(1) : bound);
        if (lower) {
            return exclusive ? comparison > 0 : comparison >= 0;
        }
        return exclusive ? comparison < 0 : comparison <= 0;
    }
}

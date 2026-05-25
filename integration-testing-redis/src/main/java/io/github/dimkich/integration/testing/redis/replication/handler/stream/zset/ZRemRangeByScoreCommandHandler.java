package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.ZRemRangeByScoreCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

@Component
public class ZRemRangeByScoreCommandHandler implements RedisStreamHandler<ZRemRangeByScoreCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == ZRemRangeByScoreCommand.class;
    }

    @Override
    public void handle(ZRemRangeByScoreCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisZSet.class, (schema, zset) -> {
            if (zset.isEmpty()) {
                return;
            }
            ScoreBoundary minBoundary = ScoreBoundary.parse(new String(event.getMin(), StandardCharsets.UTF_8), true);
            ScoreBoundary maxBoundary = ScoreBoundary.parse(new String(event.getMax(), StandardCharsets.UTF_8), false);
            zset.removeIf(entry -> {
                BigDecimal score = entry.getScore();
                return minBoundary.test(score) && maxBoundary.test(score);
            });
        });
    }

    @RequiredArgsConstructor
    private static class ScoreBoundary implements Predicate<BigDecimal> {
        private final BigDecimal limit;
        private final boolean exclusive;
        private final boolean infinite;
        private final boolean isLower;

        public static ScoreBoundary parse(String str, boolean isLower) {
            if (isLower && str.equalsIgnoreCase("-inf")) {
                return new ScoreBoundary(null, false, true, true);
            }
            if (!isLower && str.equalsIgnoreCase("+inf")) {
                return new ScoreBoundary(null, false, true, false);
            }

            boolean exclusive = str.startsWith("(");
            BigDecimal value = new BigDecimal(exclusive ? str.substring(1) : str);
            return new ScoreBoundary(value, exclusive, false, isLower);
        }

        @Override
        public boolean test(BigDecimal score) {
            if (infinite) {
                return true;
            }

            int cmp = score.compareTo(limit);
            if (isLower) {
                return exclusive ? cmp > 0 : cmp >= 0;
            } else {
                return exclusive ? cmp < 0 : cmp <= 0;
            }
        }
    }
}
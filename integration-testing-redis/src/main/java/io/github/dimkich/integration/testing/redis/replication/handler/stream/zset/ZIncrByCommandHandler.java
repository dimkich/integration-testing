package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.ZIncrByCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ZIncrByCommandHandler implements RedisStreamHandler<ZIncrByCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == ZIncrByCommand.class;
    }

    @Override
    public void handle(ZIncrByCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), RedisZSet.class, (schema, zset) -> {
            Object member = schema.getValueCodec().deserialize(event.getMember());
            BigDecimal currentScore = zset.getScore(member);
            if (currentScore == null) {
                currentScore = BigDecimal.ZERO;
            }
            BigDecimal increment = BigDecimal.valueOf(event.getIncrement());
            BigDecimal newScore = currentScore.add(increment);
            zset.put(member, newScore);
        });
    }
}
package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.AggregateType;
import com.moilioncircle.redis.replicator.cmd.impl.ZUnionStoreCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.model.RedisZSetEntry;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class ZUnionStoreCommandHandler implements RedisStreamHandler<ZUnionStoreCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == ZUnionStoreCommand.class;
    }

    @Override
    public void handle(ZUnionStoreCommand event, RedisInMemoryStore store) {
        Map<Object, BigDecimal> unionMap = new HashMap<>();
        byte[][] sourceKeys = event.getKeys();
        double[] weights = event.getWeights();

        AggregateType aggregateType = event.getAggregateType() != null
                ? event.getAggregateType()
                : AggregateType.SUM;

        for (int i = 0; i < sourceKeys.length; i++) {
            byte[] sourceKeyRaw = sourceKeys[i];
            double weight = (weights != null && i < weights.length) ? weights[i] : 1.0;

            store.compute(sourceKeyRaw, RedisZSet.class, (schema, sourceZSet) -> {
                for (RedisZSetEntry entry : sourceZSet) {
                    Object member = entry.getMember();
                    BigDecimal weightedScore = entry.getScore().multiply(BigDecimal.valueOf(weight));

                    unionMap.merge(member, weightedScore, (oldScore, newScore) -> switch (aggregateType) {
                        case MIN -> oldScore.min(newScore);
                        case MAX -> oldScore.max(newScore);
                        default -> oldScore.add(newScore);
                    });
                }
            });
        }

        if (unionMap.isEmpty()) {
            store.remove(event.getDestination());
        } else {
            store.compute(event.getDestination(), RedisZSet.class, (schema, destZSet) -> {
                destZSet.clear();
                unionMap.forEach(destZSet::put);
            });
        }
    }
}
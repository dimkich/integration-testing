package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.cmd.impl.AggregateType;
import com.moilioncircle.redis.replicator.cmd.impl.ZInterStoreCommand;
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
public class ZInterStoreCommandHandler implements RedisStreamHandler<ZInterStoreCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == ZInterStoreCommand.class;
    }

    @Override
    public void handle(ZInterStoreCommand event, RedisInMemoryStore store) {
        Map<Object, BigDecimal> intersectionMap = new HashMap<>();
        byte[][] sourceKeys = event.getKeys();
        double[] weights = event.getWeights();

        AggregateType aggregateType = event.getAggregateType() != null
                ? event.getAggregateType()
                : AggregateType.SUM;

        for (int i = 0; i < sourceKeys.length; i++) {
            byte[] sourceKeyRaw = sourceKeys[i];
            double weight = (weights != null && i < weights.length) ? weights[i] : 1.0;
            Map<Object, BigDecimal> currentSetMap = new HashMap<>();

            store.compute(sourceKeyRaw, RedisZSet.class, (schema, sourceZSet) -> {
                for (RedisZSetEntry entry : sourceZSet) {
                    currentSetMap.put(entry.getMember(), entry.getScore().multiply(BigDecimal.valueOf(weight)));
                }
            });

            if (i == 0) {
                intersectionMap.putAll(currentSetMap);
            } else {
                intersectionMap.keySet().retainAll(currentSetMap.keySet());
                intersectionMap.forEach((member, score) -> {
                    BigDecimal nextScore = currentSetMap.get(member);
                    intersectionMap.put(member, switch (aggregateType) {
                        case MIN -> score.min(nextScore);
                        case MAX -> score.max(nextScore);
                        default -> score.add(nextScore);
                    });
                });
            }
            if (intersectionMap.isEmpty()) {
                break;
            }
        }

        if (intersectionMap.isEmpty()) {
            store.remove(event.getDestination());
        } else {
            store.compute(event.getDestination(), RedisZSet.class, (schema, destZSet) -> {
                destZSet.clear();
                intersectionMap.forEach(destZSet::put);
            });
        }
    }
}
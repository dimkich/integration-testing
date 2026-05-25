package io.github.dimkich.integration.testing.redis.replication.handler.stream.zset;

import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.model.RedisZSetEntry;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ZRangeStoreCommandHandler implements RedisStreamHandler<ZRangeStoreCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == ZRangeStoreCommand.class;
    }

    @Override
    public void handle(ZRangeStoreCommand event, RedisInMemoryStore store) {
        List<RedisZSetEntry> resultList = new ArrayList<>();

        store.compute(event.getSrc(), RedisZSet.class, (schema, srcZSet) -> {
            if (srcZSet.isEmpty()) {
                return;
            }

            List<RedisZSetEntry> list = new ArrayList<>(srcZSet);

            if (event.getRangeType() != ZRangeStoreCommand.RangeType.RANK) {
                String v1 = new String(event.getMin(), StandardCharsets.UTF_8);
                String v2 = new String(event.getMax(), StandardCharsets.UTF_8);

                list = list.stream().filter(entry -> {
                    if (event.getRangeType() == ZRangeStoreCommand.RangeType.SCORE) {
                        return isInsideScoreRange(entry.getScore(), v1, v2);
                    } else {
                        return isInsideLexRange(entry.getMember().toString(), v1, v2);
                    }
                }).collect(Collectors.toCollection(ArrayList::new));
            }

            if (event.isRev()) {
                list.sort(Collections.reverseOrder());
            } else {
                Collections.sort(list);
            }

            if (event.getRangeType() == ZRangeStoreCommand.RangeType.RANK) {
                list = applyRankSlice(list, event);
            } else if (event.getLimit() != null) {
                int offset = (int) event.getLimit().getOffset();
                int count = (int) event.getLimit().getCount();
                list = list.stream()
                        .skip(Math.max(0, offset))
                        .limit(count < 0 ? list.size() : count)
                        .collect(Collectors.toList());
            }

            resultList.addAll(list);
        });

        if (resultList.isEmpty()) {
            store.remove(event.getDest());
        } else {
            store.compute(event.getDest(), RedisZSet.class, (destSchema, destZSet) -> {
                destZSet.clear();
                destZSet.addAll(resultList);
            });
        }
    }

    private boolean isInsideScoreRange(BigDecimal score, String v1, String v2) {
        double d1 = parseScoreValue(v1);
        double d2 = parseScoreValue(v2);

        String lower = (d1 <= d2) ? v1 : v2;
        String upper = (d1 <= d2) ? v2 : v1;

        return checkScoreBound(score, lower, true) && checkScoreBound(score, upper, false);
    }

    private double parseScoreValue(String v) {
        if (v.equalsIgnoreCase("-inf")) {
            return Double.NEGATIVE_INFINITY;
        }
        if (v.equalsIgnoreCase("+inf")) {
            return Double.POSITIVE_INFINITY;
        }
        return Double.parseDouble(v.replace("(", ""));
    }

    private boolean checkScoreBound(BigDecimal score, String bound, boolean isLower) {
        if (isLower && bound.equalsIgnoreCase("-inf")) {
            return true;
        }
        if (!isLower && bound.equalsIgnoreCase("+inf")) {
            return true;
        }

        boolean inclusive = !bound.startsWith("InsideScoreRange");
        if (bound.startsWith("(")) {
            inclusive = false;
        }

        double val = Double.parseDouble(bound.replace("(", ""));
        double s = score.doubleValue();

        if (isLower) {
            return inclusive ? s >= val : s > val;
        } else {
            return inclusive ? s <= val : s < val;
        }
    }

    private boolean isInsideLexRange(String member, String v1, String v2) {
        return checkLexBound(member, v1, true) && checkLexBound(member, v2, false);
    }

    private boolean checkLexBound(String member, String bound, boolean isLower) {
        if (isLower && bound.equals("-")) {
            return true;
        }
        if (!isLower && bound.equals("+")) {
            return true;
        }

        boolean inclusive = !bound.startsWith("(");
        String val = (bound.startsWith("(") || bound.startsWith("[")) ? bound.substring(1) : bound;
        int cmp = member.compareTo(val);

        if (isLower) {
            return inclusive ? cmp >= 0 : cmp > 0;
        } else {
            return inclusive ? cmp <= 0 : cmp < 0;
        }
    }

    private List<RedisZSetEntry> applyRankSlice(List<RedisZSetEntry> list, ZRangeStoreCommand event) {
        int size = list.size();
        try {
            int start = Integer.parseInt(new String(event.getMin(), StandardCharsets.UTF_8));
            int stop = Integer.parseInt(new String(event.getMax(), StandardCharsets.UTF_8));

            if (start < 0) {
                start += size;
            }
            if (stop < 0) {
                stop += size;
            }
            start = Math.max(0, start);
            if (start >= size || start > stop) {
                return Collections.emptyList();
            }
            stop = Math.min(size - 1, stop);
            return list.subList(start, stop + 1);
        } catch (NumberFormatException e) {
            return Collections.emptyList();
        }
    }
}
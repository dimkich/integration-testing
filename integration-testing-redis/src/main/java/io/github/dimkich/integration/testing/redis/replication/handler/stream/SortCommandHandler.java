package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.impl.OrderType;
import com.moilioncircle.redis.replicator.cmd.impl.SortCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.model.*;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
public class SortCommandHandler implements RedisStreamHandler<SortCommand> {

    private static final RedisDataComparator REDIS_COMPARATOR = new RedisDataComparator();

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == SortCommand.class;
    }

    @Override
    public void handle(SortCommand event, RedisInMemoryStore store) {
        if (event.getDestination() == null) {
            return;
        }

        store.compute(event.getKey(), (schema, entry) -> {
            Object data = entry.getData();
            if (data == null) {
                store.remove(event.getDestination());
                return;
            }

            List<Object> rawElements = new ArrayList<>();
            if (data instanceof List<?> list) {
                rawElements.addAll(list);
            } else if (data instanceof RedisZSet zset) {
                zset.forEach(z -> rawElements.add(z.getMember()));
            } else if (data instanceof Set<?> set) {
                rawElements.addAll(set);
            } else {
                return;
            }

            byte[] byPattern = event.getByPattern();
            boolean skipSort = (byPattern != null && !hasStar(byPattern));

            List<SortItem> items = new ArrayList<>();
            for (Object element : rawElements) {
                Object sortValue = element;
                if (byPattern != null && !skipSort) {
                    sortValue = fetchValue(byPattern, element, schema, store);
                }
                items.add(new SortItem(element, sortValue));
            }

            if (!skipSort) {
                items.sort(createComparator(event));
            }

            List<SortItem> limitedResult = applyLimit(items, event);
            List<Object> finalResult = new ArrayList<>();

            byte[][] getPatterns = event.getGetPatterns();
            if (getPatterns != null && getPatterns.length > 0) {
                for (SortItem item : limitedResult) {
                    for (byte[] getPattern : getPatterns) {
                        if (getPattern.length == 1 && getPattern[0] == '#') {
                            finalResult.add(item.element);
                        } else {
                            finalResult.add(fetchValue(getPattern, item.element, schema, store));
                        }
                    }
                }
            } else {
                for (SortItem item : limitedResult) {
                    finalResult.add(item.element);
                }
            }

            if (!finalResult.isEmpty()) {
                store.compute(event.getDestination(), RedisList.class, (destSchema, list) -> {
                    list.clear();
                    list.addAll(finalResult);
                });
            } else {
                store.remove(event.getDestination());
            }
        });
    }

    private record SortItem(Object element, Object sortValue) {
    }

    private Object fetchValue(byte[] pattern, Object element, RedisDataSchema schema, RedisInMemoryStore store) {
        byte[] elementBytes = schema.getValueCodec().serialize(element);
        byte[] fullKey = buildKey(pattern, elementBytes);
        byte[][] parts = splitHashPattern(fullKey);

        Object[] result = new Object[1];
        store.compute(parts[0], (s, entry) -> {
            if (parts.length == 1) {
                result[0] = entry.getData();
            } else if (entry.getData() instanceof RedisHash hash) {
                Object fieldKey = s.getHashKeyCodec().deserialize(parts[1]);
                RedisEntry fieldEntry = hash.get(fieldKey);
                if (fieldEntry != null) {
                    result[0] = fieldEntry.getData();
                }
            }
        });
        return result[0];
    }

    private boolean hasStar(byte[] pattern) {
        for (byte b : pattern) {
            if (b == '*') {
                return true;
            }
        }
        return false;
    }

    private byte[] buildKey(byte[] pattern, byte[] elementBytes) {
        int starIdx = -1;
        for (int i = 0; i < pattern.length; i++) {
            if (pattern[i] == '*') {
                starIdx = i;
                break;
            }
        }
        if (starIdx == -1) {
            return pattern;
        }

        byte[] result = new byte[pattern.length - 1 + elementBytes.length];
        System.arraycopy(pattern, 0, result, 0, starIdx);
        System.arraycopy(elementBytes, 0, result, starIdx, elementBytes.length);
        System.arraycopy(pattern, starIdx + 1, result, starIdx + elementBytes.length, pattern.length - starIdx - 1);
        return result;
    }

    private byte[][] splitHashPattern(byte[] keyBytes) {
        for (int i = 0; i < keyBytes.length - 1; i++) {
            if (keyBytes[i] == '-' && keyBytes[i + 1] == '>') {
                byte[] keyPart = new byte[i];
                System.arraycopy(keyBytes, 0, keyPart, 0, i);
                byte[] fieldPart = new byte[keyBytes.length - i - 2];
                System.arraycopy(keyBytes, i + 2, fieldPart, 0, fieldPart.length);
                return new byte[][]{keyPart, fieldPart};
            }
        }
        return new byte[][]{keyBytes};
    }

    private Comparator<SortItem> createComparator(SortCommand event) {
        Comparator<SortItem> comparator = (a, b) -> {
            Object v1 = a.sortValue();
            Object v2 = b.sortValue();
            if (event.isAlpha()) {
                return REDIS_COMPARATOR.compare(v1, v2);
            }
            return Double.compare(toDouble(v1), toDouble(v2));
        };
        return event.getOrder() == OrderType.DESC ? comparator.reversed() : comparator;
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        return Double.parseDouble(value.toString());
    }

    private List<SortItem> applyLimit(List<SortItem> source, SortCommand event) {
        if (source.isEmpty() || event.getLimit() == null) {
            return source;
        }
        int offset = Math.max(0, (int) event.getLimit().getOffset());
        int count = (int) event.getLimit().getCount();
        if (offset >= source.size()) {
            return List.of();
        }
        int end = (count < 0) ? source.size() : Math.min(source.size(), offset + count);
        return source.subList(offset, end);
    }
}
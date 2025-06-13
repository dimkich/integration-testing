package io.github.dimkich.integration.testing.redis.redisson;

import org.apache.commons.lang3.tuple.Pair;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RObject;
import org.redisson.client.codec.Codec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedissonClientAnswer implements Answer<Object> {
    private static final RedissonMockFactory redissonMockFactory = new RedissonMockFactory();
    private final Map<Pair<Class<? extends RObject>, String>, RObject> rObjectMap = new ConcurrentHashMap<>();

    @Override
    public Object answer(InvocationOnMock invocation) {
        Codec codec = invocation.getArguments().length > 1 && invocation.getArguments()[1] instanceof Codec
                ? (Codec) invocation.getArguments()[1] : null;
        String name = (String) invocation.getArguments()[0];

        Class<? extends RObject> mockClass;
        RedissonObjectAnswer answer = switch (invocation.getMethod().getName()) {
            case "getMap" -> {
                mockClass = RMap.class;
                yield new RedissonObjectAnswer(new ConcurrentHashMap<>(), name, codec,
                        redissonMockFactory.getMap());
            }
            case "getMapCache" -> {
                mockClass = RMapCache.class;
                yield new RedissonObjectAnswer(new ConcurrentHashMap<>(), name, codec,
                        redissonMockFactory.getMapCache());
            }
            case "getBucket" -> {
                mockClass = RBucket.class;
                yield new RedissonObjectAnswer(new ValueHolder(), name, codec, redissonMockFactory.getBucket());
            }
            default -> throw new RuntimeException("RedissonClient method " + invocation.getMethod().getName()
                                                  + " not supported");
        };

        Class<? extends RObject> mc = mockClass;
        return rObjectMap.computeIfAbsent(Pair.of(mockClass, name),
                p -> Mockito.mock(mc, Mockito.withSettings().defaultAnswer(answer)));
    }
}

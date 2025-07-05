package io.github.dimkich.integration.testing.redis.redisson;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RObject;
import org.redisson.client.codec.Codec;
import org.redisson.command.CommandAsyncService;
import org.redisson.config.Config;
import org.redisson.eviction.EvictionScheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class RedissonClientAnswer implements Answer<Object> {
    private final Config config;
    public static final RedissonMockFactory redissonMockFactory = new RedissonMockFactory();
    private final Map<Pair<Class<? extends RObject>, String>, RObject> rObjectMap = new ConcurrentHashMap<>();
    @Setter
    private RedissonDataStorage storage;

    @Override
    public Object answer(InvocationOnMock invocation) {
        if (invocation.getArguments().length == 0) {
            return switch (invocation.getMethod().getName()) {
                case "getConfig" -> config;
                case "getCommandExecutor" -> Mockito.mock(CommandAsyncService.class, Answers.RETURNS_DEEP_STUBS);
                case "getEvictionScheduler" -> Mockito.mock(EvictionScheduler.class, Answers.RETURNS_DEEP_STUBS);
                default -> throw new IllegalStateException("Unexpected invocation: " + invocation.getMethod());
            };
        }
        Codec codec = invocation.getArguments().length > 1 && invocation.getArguments()[1] instanceof Codec
                ? (Codec) invocation.getArguments()[1] : null;
        String name = (String) invocation.getArguments()[0];

        Class<? extends RObject> mockClass;
        RedissonObjectAnswer answer = switch (invocation.getMethod().getName()) {
            case "getMap" -> {
                mockClass = RMap.class;
                yield new RedissonObjectAnswer(new ConcurrentHashMap<>(), name, codec,
                        redissonMockFactory.getMap(), null);
            }
            case "getMapCache" -> {
                mockClass = RMapCache.class;
                yield new RedissonObjectAnswer(new ConcurrentHashMap<>(), name, codec,
                        redissonMockFactory.getMapCache(), null);
            }
            case "getBucket" -> {
                mockClass = RBucket.class;
                yield new RedissonObjectAnswer(new ValueHolder(), name, codec, redissonMockFactory.getBucket(), null);
            }
            default -> throw new RuntimeException("RedissonClient method " + invocation.getMethod().getName()
                    + " not supported");
        };

        Class<? extends RObject> mc = mockClass;
        RObject object = rObjectMap.computeIfAbsent(Pair.of(mockClass, name),
                p -> Mockito.mock(mc, Mockito.withSettings().defaultAnswer(answer)));
        if (storage != null) {
            storage.tryPut(object);
        }
        return object;
    }
}

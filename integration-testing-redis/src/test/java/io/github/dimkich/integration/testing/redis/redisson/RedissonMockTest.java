package io.github.dimkich.integration.testing.redis.redisson;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.redisson.Redisson;
import org.redisson.RedissonObject;
import org.redisson.api.*;
import org.redisson.jcache.JCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {RedissonConfig.class, RedissonMockTest.Config.class},
        properties = {"integration.testing.environment=mock"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class RedissonMockTest {
    private final RedissonClient redisson;
    private final Set<String> objectMethods = Arrays.stream(Object.class.getMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());

    static Object[][] bucketData() {
        return new Object[][]{
                {"set", new Object[]{"v"}, null},
                {"size", new Object[]{}, (long) Integer.MAX_VALUE},
                {"set", new Object[]{"v"}, null},
                {"get", new Object[]{}, "v"},
                {"getAndClearExpire", new Object[]{}, "v"},
                {"getAndExpire", new Object[]{ZonedDateTime.now().toInstant()}, "v"},
                {"compareAndSet", new Object[]{"v1", "v2"}, false},
                {"compareAndSet", new Object[]{"v", "v1"}, true},
                {"get", new Object[]{}, "v1"},
                {"getAndSet", new Object[]{"v2"}, "v1"},
                {"get", new Object[]{}, "v2"},
                {"setAndKeepTTL", new Object[]{"v3"}, null},
                {"getAndDelete", new Object[]{}, "v3"},
                {"setIfAbsent", new Object[]{"v1"}, true},
                {"get", new Object[]{}, "v1"},
                {"setIfAbsent", new Object[]{"v2"}, false},
                {"get", new Object[]{}, "v1"},
                {"setIfExists", new Object[]{"v2"}, true},
                {"get", new Object[]{}, "v2"},
        };
    }

    @ParameterizedTest
    @MethodSource("bucketData")
    void bucket(String method, Object[] args, Object value) throws ExecutionException, InterruptedException,
    InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        RBucket<String> bucket = redisson.getBucket("test");
        assertEquals(value, getMethodResult(bucket, method, args));
    }

    static Object[][] mapCacheData() {
        return new Object[][]{
                {"put", new Object[]{"key", "value"}, null},
                {"get", new Object[]{"key"}, "value"},
                {"size", new Object[]{}, 1},
                {"putAsync", new Object[]{"key1", "value1"}, null},
                {"getAsync", new Object[]{"key"}, "value"},
                {"getAsync", new Object[]{"key1"}, "value1"},
                {"size", new Object[]{}, 2},
                {"getAll", new Object[]{Set.of("key")}, Map.of("key", "value")},
                {"getAllAsync", new Object[]{Set.of("key1")}, Map.of("key1", "value1")},
                {"readAllMap", new Object[]{}, Map.of("key", "value", "key1", "value1")},
                {"fastRemove", new Object[]{new Object[]{"key1"}}, 1L},
                {"size", new Object[]{}, 1},
                {"clear", new Object[]{}, null},
                {"size", new Object[]{}, 0},

                {"putIfAbsent", new Object[]{"k1", "v1"}, null},
                {"putIfAbsent", new Object[]{"k1", "v2"}, "v1"},
                {"get", new Object[]{"k1"}, "v1"},
                {"fastPutIfAbsent", new Object[]{"k", "a"}, true},
                {"get", new Object[]{"k"}, "a"},
                {"fastPutIfAbsent", new Object[]{"k", "a1"}, false},
                {"get", new Object[]{"k"}, "a"},
                {"readAllKeySet", new Object[]{}, Set.of("k1", "k")},
                {"keySet", new Object[]{}, Set.of("k1", "k")},
                {"readAllValues", new Object[]{}, Set.of("v1", "a")},
                {"values", new Object[]{}, Set.of("v1", "a")},
                {"readAllEntrySet", new Object[]{}, Set.of(new AbstractMap.SimpleEntry<>("k1", "v1"),
                        new AbstractMap.SimpleEntry<>("k", "a"))},
                {"entrySet", new Object[]{}, Set.of(new AbstractMap.SimpleEntry<>("k1", "v1"),
                        new AbstractMap.SimpleEntry<>("k", "a"))},
                {"remove", new Object[]{"key1"}, null},
                {"isEmpty", new Object[]{}, false},
                {"remove", new Object[]{"k"}, "a"},
                {"isEmpty", new Object[]{}, false},
                {"remove", new Object[]{"k1"}, "v1"},
                {"isEmpty", new Object[]{}, true},

                {"containsKey", new Object[]{"k1"}, false},
                {"containsValue", new Object[]{"v2"}, false},
                {"putAll", new Object[]{Map.of("k1", "v1", "k2", "v2")}, null},
                {"containsKey", new Object[]{"k1"}, true},
                {"containsValue", new Object[]{"v2"}, true},
                {"get", new Object[]{"k1"}, "v1"},
                {"get", new Object[]{"k2"}, "v2"},
                {"merge", new Object[]{"k1", "n", (BiFunction<String, String, String>) (v1, v2) -> v1 + v2}, "v1n"},
                {"get", new Object[]{"k1"}, "v1n"},
                {"computeIfAbsent", new Object[]{"k1", (Function<String, String>) (k1) -> k1}, "v1n"},
                {"get", new Object[]{"k1"}, "v1n"},
                {"computeIfAbsent", new Object[]{"k3", (Function<String, String>) (k1) -> k1}, "k3"},
                {"get", new Object[]{"k3"}, "k3"},
                {"computeIfPresent", new Object[]{"k4", (BiFunction<String, String, String>) (v1, v2) -> v1 + v2}, null},
                {"get", new Object[]{"k4"}, null},
                {"computeIfPresent", new Object[]{"k3", (BiFunction<String, String, String>) (k, v) -> k + v}, "k3k3"},
                {"get", new Object[]{"k3"}, "k3k3"},
                {"compute", new Object[]{"k3", (BiFunction<String, String, String>) (k, v) -> k + v}, "k3k3k3"},
                {"compute", new Object[]{"k4", (BiFunction<String, String, String>) (k, v) -> k + v}, "k4null"},
                {"get", new Object[]{"k4"}, "k4null"},
                {"replace", new Object[]{"k4", "v1", "v4"}, false},
                {"replace", new Object[]{"k4", "k4null", "v4"}, true},
                {"getOrDefault", new Object[]{"k4", "v"}, "v4"},
                {"getOrDefault", new Object[]{"k5", "v"}, "v"},
                {"clear", new Object[]{}, null},
                {"isEmpty", new Object[]{}, true},

                {"putIfAbsent", new Object[]{"k1", "v1"}, null},
                {"putIfAbsent", new Object[]{"k1", "v2"}, "v1"},
                {"get", new Object[]{"k1"}, "v1"},
                {"putIfExists", new Object[]{"k2", "v2"}, null},
                {"putIfExists", new Object[]{"k1", 1}, 1},
                {"addAndGet", new Object[]{"k1", 2}, 3},
                {"get", new Object[]{"k1"}, 3},
                {"put", new Object[]{"k1", 10L}, 3},
                {"addAndGet", new Object[]{"k1", 4L}, 14L},
                {"get", new Object[]{"k1"}, 14L},
                {"put", new Object[]{"k1", 2.3}, 14L},
                {"addAndGet", new Object[]{"k1", 3.2}, 5.5},
                {"get", new Object[]{"k1"}, 5.5},
                {"put", new Object[]{"k1", 1.3f}, 5.5},
                {"addAndGet", new Object[]{"k1", 2.2f}, 3.5f},
                {"get", new Object[]{"k1"}, 3.5f},
        };
    }

    @ParameterizedTest
    @MethodSource("mapCacheData")
    void mapCache(String method, Object[] args, Object value) throws ExecutionException, InterruptedException,
            InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        RMapCache<Object, Object> mapCache = redisson.getMapCache("test");
        assertEquals(value, getMethodResult(mapCache, method, args));
    }

    private Object getMethodResult(Object object, String method, Object[] args) throws InvocationTargetException, IllegalAccessException, ExecutionException, InterruptedException, NoSuchMethodException {
        Class<?>[] classes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
        Method m = MethodUtils.getMatchingAccessibleMethod(object.getClass(), method, classes);
        if (m == null) {
            throw new NoSuchMethodException("Method " + method + Arrays.toString(classes) + " not found");
        }
        Object result = m.invoke(object, args);
        if (result instanceof RFuture<?> future) {
            result = future.get();
        }
        if (result instanceof Collection<?> set) {
            Set<Object> s = new TreeSet<>(Comparator.comparing(Object::toString));
            s.addAll(set);
            result = s;
        }
        return result;
    }

    @Test
    void testCreation() {
        RMapCache<String, String> mapCache = redisson.getMapCache("test");
        mapCache.put("k1", "v1");
        assertEquals("v1", mapCache.get("k1"));
        assertEquals(mapCache, redisson.getMapCache("test"));

        RMap<String, String> map = redisson.getMap("test");
        mapCache.put("k1", "v1");
        assertEquals("v1", mapCache.get("k1"));
        assertEquals(map, redisson.getMap("test"));

        RBucket<String> bucket = redisson.getBucket("test");
        bucket.set("v1");
        assertEquals("v1", bucket.get());
        assertEquals(bucket, redisson.getBucket("test"));

        mapCache.clear();
        map.clear();
        bucket.set(null);
    }

    static Object[][] methodsSupportData() {
        RedissonMockFactory factory = new RedissonMockFactory();
        return new Object[][]{
                {ConcurrentMap.class, factory.getConcurrentMap()},
                {RObject.class, factory.getObject()},
                {RDestroyable.class, factory.getDestroyable()},
                {RExpirable.class, factory.getExpirable()},
                {RedissonObject.class, factory.getRedissonObject()},
                {RBucket.class, factory.getBucket()},
                {RMap.class, factory.getMap()},
                {RMapCache.class, factory.getMapCache()},
                {JCache.class, factory.getJCache()},
        };
    }

    @ParameterizedTest
    @MethodSource("methodsSupportData")
    void methodsSupport(Class<?> cls, RedissonMock mock) {
        for (Method method : cls.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || objectMethods.contains(method.getName())) {
                continue;
            }
            String methodName = method.getName();
            if (methodName.endsWith("Async")) {
                methodName = methodName.substring(0, methodName.length() - 5);
            }
            Assertions.assertTrue(mock.getMethods().contains(methodName),
                    String.format("Method %s is not implemented for %s", method.getName(), mock.getTargetClass()));
        }
    }

    @Configuration
    static class Config {
        @Bean
        RedissonClient redisson() {
            return Redisson.create();
        }
    }
}
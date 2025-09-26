package io.github.dimkich.integration.testing.redis.redisson;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import io.netty.buffer.ByteBuf;
import lombok.SneakyThrows;
import org.redisson.RedissonObject;
import org.redisson.api.*;
import org.redisson.jcache.JCache;
import org.redisson.jcache.JCacheEntry;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorResult;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RedissonMockFactory {
    private static final byte[] ZERO_BYTE_ARRAY = new byte[0];
    private static final Function<RMockInvoke, Object> NULL = mi -> null;
    private static final Function<RMockInvoke, Object> ZERO = mi -> 0;
    private static final Function<RMockInvoke, Object> INT_MAX = mi -> Integer.MAX_VALUE;
    private static final Function<RMockInvoke, Object> TRUE = mi -> true;
    private static final Function<RMockInvoke, Object> FALSE = mi -> false;
    private static final Function<RMockInvoke, Object> BYTE_ARRAY = mi -> ZERO_BYTE_ARRAY;

    private RedissonMock object;
    private RedissonMock destroyable;
    private RedissonMock expirable;
    private RedissonMock redissonObject;
    private RedissonMock bucket;
    private RedissonMock map;
    private RedissonMock mapCache;
    private RedissonMock jCache;
    private RedissonMock lock;
    private RedissonMock concurrentMap;

    public RedissonMock getObject() {
        if (object == null) {
            object = new RedissonMock(RObject.class).add("getIdleTime", ZERO).add("sizeInMemory", ZERO)
                    .add("restore", NULL).add("restoreAndReplace", NULL).add("dump", BYTE_ARRAY)
                    .add("touch", TRUE).add("migrate", NULL).add("copy", NULL)
                    .add("move", FALSE).add("getName", RMockInvoke::getName).add("delete", FALSE)
                    .add("unlink", FALSE).add("rename", NULL).add("renamenx", FALSE)
                    .add("isExists", TRUE).add("getCodec", RMockInvoke::getCodec)
                    .add("addListener", NULL).add("removeListener", NULL);
        }
        return object;
    }

    public RedissonMock getDestroyable() {
        if (destroyable == null) {
            destroyable = new RedissonMock(RDestroyable.class).add("destroy", NULL);
        }
        return destroyable;
    }

    public RedissonMock getExpirable() {
        if (expirable == null) {
            expirable = new RedissonMock(RExpirable.class).add("expireAt", FALSE).add("expire", FALSE)
                    .add("expireIfSet", FALSE).add("expireIfNotSet", FALSE)
                    .add("expireIfGreater", FALSE).add("expireIfLess", FALSE)
                    .add("clearExpire", TRUE).add("remainTimeToLive", INT_MAX)
                    .add("getExpireTime", INT_MAX).add(getObject());
        }
        return expirable;
    }

    public RedissonMock getRedissonObject() {
        if (redissonObject == null) {
            redissonObject = new RedissonMock(RedissonObject.class).add("getLockByValue", NULL)
                    .add("getRawName", RMockInvoke::getName).add("getLockByMapKey", NULL)
                    .add("encodeMapKey",
                            SneakyFunction.sneaky(mc -> mc.getCodec().getMapKeyEncoder().encode(mc.getArg1())))
                    .add("encodeMapValue",
                            SneakyFunction.sneaky(mc -> mc.getCodec().getMapValueEncoder().encode(mc.getArg1())))
                    .add("encode", new Function<>() {
                        @Override
                        @SneakyThrows
                        @SuppressWarnings("unchecked")
                        public Object apply(RMockInvoke mc) {
                            if (mc.getInvocation().getArguments().length == 1) {
                                if (mc.getArg1() instanceof Collection<?> values) {
                                    List<ByteBuf> result = new ArrayList<>(values.size());
                                    for (Object object : values) {
                                        result.add(mc.getCodec().getValueEncoder().encode(object));
                                    }
                                    return result;
                                }
                                return mc.getCodec().getValueEncoder().encode(mc.getArg1());
                            }
                            if (mc.getArg1() instanceof Collection params && mc.getArg2() instanceof Collection<?> values) {
                                for (Object object : values) {
                                    params.add(mc.getCodec().getValueEncoder().encode(object));
                                }
                                return null;
                            }
                            Object v = mc.getCodec().getValueEncoder().encode(mc.getArg2());
                            ((Collection<Object>) mc.getArg1()).add(v);
                            return null;
                        }
                    })
                    .add(getObject());
        }
        return redissonObject;
    }

    public RedissonMock getBucket() {
        if (bucket == null) {
            bucket = new RedissonMock(RBucket.class)
                    .add("get", mc -> mc.valueHolder().getValue())
                    .add("getAndClearExpire", mc -> mc.valueHolder().getValue())
                    .add("getAndDelete", mc -> {
                        Object o = mc.valueHolder().getValue();
                        mc.valueHolder().setValue(null);
                        return o;
                    })
                    .add("getAndExpire", mc -> mc.valueHolder().getValue())
                    .add("size", mc -> (long) Integer.MAX_VALUE)
                    .add("compareAndSet", mc -> {
                        if (mc.valueHolder().getValue() == mc.getArg1()) {
                            mc.valueHolder().setValue(mc.getArg2());
                            return true;
                        }
                        return false;
                    })
                    .add("getAndSet", mc -> {
                        Object old = mc.valueHolder().getValue();
                        mc.valueHolder().setValue(mc.getArg1());
                        return old;
                    })
                    .add("set", mc -> {
                        mc.valueHolder().setValue(mc.getArg1());
                        return null;
                    })
                    .add("setAndKeepTTL", mc -> {
                        mc.valueHolder().setValue(mc.getArg1());
                        return null;
                    })
                    .add("setIfAbsent", mc -> {
                        if (mc.valueHolder().getValue() == null) {
                            mc.valueHolder().setValue(mc.getArg1());
                            return true;
                        }
                        return false;
                    })
                    .add("setIfExists", mc -> {
                        if (mc.valueHolder().getValue() != null) {
                            mc.valueHolder().setValue(mc.getArg1());
                            return true;
                        }
                        return false;
                    })
                    .add("trySet", mc -> {
                        if (mc.valueHolder().getValue() == null) {
                            mc.valueHolder().setValue(mc.getArg1());
                            return true;
                        }
                        return false;
                    }).add(getObject()).add(getExpirable());
        }
        return bucket;
    }

    public RedissonMock getMap() {
        if (map == null) {
            map = new RedissonMock(RMap.class)
                    .add("loadAll", NULL).add("randomKeys", NULL)
                    .add("putIfExists", mc -> mc.concurrentMap()
                            .computeIfPresent(mc.getArg1(), (k, v) -> mc.getArg2()))
                    .add("randomEntriMethodCalles", NULL).add("mapReduce", NULL)
                    .add("getCountDownLatch", NULL).add("getPermitExpirableSemaphore", NULL)
                    .add("getSemaphore", NULL).add("getFairLock", NULL)
                    .add("getReadWriteLock", NULL).add("getLock", NULL).add("valueSize", ZERO)
                    .add("addAndGet", mc -> mc.concurrentMap()
                            .computeIfPresent(mc.getArg1(), (k, v) -> addNumbers((Number) v, (Number) mc.getArg2())))
                    .add("getAll", mc -> mc.concurrentMap().entrySet().stream()
                            .filter(e -> ((Set<?>) mc.getArg1()).contains(e.getKey()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    .add("readAllMap", RMockInvoke::getMock)
                    .add("fastRemove", mc -> mc.getArgs().map(k -> mc.concurrentMap().remove(k))
                            .filter(Objects::nonNull).count())
                    .add("fastPut", mc -> mc.rMap().put(mc.getArg1(), mc.getArg2()))
                    .add("fastReplace", mc -> mc.concurrentMap().replace(mc.getArg1(), mc.getArg2()))
                    .add("fastPutIfAbsent", mc -> mc.rMap().putIfAbsent(mc.getArg1(), mc.getArg2()) == null)
                    .add("fastPutIfExists", mc -> mc.rMap().putIfExists(mc.getArg1(), mc.getArg2()))
                    .add("readAllKeySet", mc -> mc.rMap().keySet())
                    .add("readAllValues", mc -> mc.rMap().values())
                    .add("readAllEntrySet", mc -> mc.rMap().entrySet())
                    .add("randomEntries", NULL)
                    .add(getConcurrentMap()).add(getExpirable()).add(getDestroyable());
        }
        return map;
    }

    public RedissonMock getMapCache() {
        if (mapCache == null) {
            mapCache = new RedissonMock(RMapCache.class).add("setMaxSize", NULL).add("trySetMaxSize", NULL)
                    .add("updateEntryExpiration", FALSE).add("addListener", NULL)
                    .add("removeListener", NULL).add("getWithTTLOnly", mc -> mc.rMap().get(mc.getArg1()))
                    .add(getMap());
        }
        return mapCache;
    }

    @SuppressWarnings("unchecked")
    public RedissonMock getJCache() {
        if (jCache == null) {
            jCache = new RedissonMock(JCache.class).add("invokeAll", mc -> {
                        Map<Object, EntryProcessorResult<Object>> result = new LinkedHashMap<>();
                        for (Object key : ((Set<?>) mc.getArg1())) {
                            Object pr;
                            if (mc.getArgCount() == 2) {
                                pr = ((EntryProcessor) mc.getArg2()).process(
                                        new MapMutableEntry<>(mc.getArg1(), mc.concurrentMap()));
                            } else {
                                pr = ((EntryProcessor) mc.getArg2()).process(
                                        new MapMutableEntry<>(mc.getArg1(), mc.concurrentMap()), (Object[]) mc.getArg3());
                            }
                            result.put(key, () -> pr);
                        }
                        return result;
                    })
                    .add("invoke", mc -> {
                        if (mc.getArgCount() == 2) {
                            return ((EntryProcessor) mc.getArg2()).process(
                                    new MapMutableEntry<>(mc.getArg1(), mc.concurrentMap()));
                        } else {
                            return ((EntryProcessor) mc.getArg2()).process(
                                    new MapMutableEntry<>(mc.getArg1(), mc.concurrentMap()), (Object[]) mc.getArg3());
                        }
                    })
                    .add("close", NULL).add("isClosed", FALSE)
                    .add("deregisterCacheEntryListener", NULL).add("unwrap", NULL)
                    .add("registerCacheEntryListener", NULL).add("getCacheManager", NULL)
                    .add("getConfiguration", RMockInvoke::getConfig)
                    .add("getAndPut", mc -> mc.concurrentMap().put(mc.getArg1(), mc.getArg2()))
                    .add("getAndReplace", mc -> mc.concurrentMap().put(mc.getArg1(), mc.getArg2()))
                    .add("getAndRemove", mc -> mc.concurrentMap().remove(mc.getArg1()))
                    .add("iterator", mc -> mc.concurrentMap().entrySet().stream()
                            .map(e -> new JCacheEntry<>(e.getKey(), e.getValue())).iterator())
                    .add("spliterator", mc -> mc.concurrentMap().entrySet().stream()
                            .map(e -> new JCacheEntry<>(e.getKey(), e.getValue())).spliterator())
                    .add("removeAll",
                            mc -> {
                                ((Collection<?>) mc.getArg1()).forEach(v -> mc.concurrentMap().remove(v));
                                return null;
                            })
                    .add(getRedissonObject()).add(getMap());
        }
        return jCache;
    }

    public RedissonMock getLock() {
        if (lock == null) {
            lock = new RedissonMock(RLock.class).add("getName", RMockInvoke::getName)
                    .add("isLocked", mc -> mc.lock().isLocked()).add("isHeldByThread", FALSE)
                    .add("isHeldByCurrentThread", mc -> mc.lock().isHeldByCurrentThread())
                    .add("getHoldCount", mc -> mc.lock().getHoldCount())
                    .add("remainTimeToLive", mc -> -1).add("newCondition", mc -> mc.lock().newCondition())
                    .add("lockInterruptibly", mc -> {
                        mc.lock().lock();
                        return null;
                    }).add("tryLock", mc -> {
                        mc.lock().lock();
                        return null;
                    }).add("lock", mc -> {
                        mc.lock().lock();
                        return null;
                    }).add("forceUnlock", mc -> {
                        mc.lock().unlock();
                        return null;
                    }).add("unlock", mc -> {
                        mc.lock().unlock();
                        return null;
                    });
        }
        return lock;
    }

    RedissonMock getConcurrentMap() {
        if (concurrentMap == null) {
            concurrentMap = new RedissonMock(ConcurrentMap.class).add("size", RMockInvoke::proxyCall0)
                    .add("isEmpty", RMockInvoke::proxyCall0).add("containsKey", RMockInvoke::proxyCall1)
                    .add("containsValue", RMockInvoke::proxyCall1).add("get", RMockInvoke::proxyCall1)
                    .add("put", RMockInvoke::proxyCall2).add("putAll", RMockInvoke::proxyCall1)
                    .add("clear", RMockInvoke::proxyCall0).add("keySet", RMockInvoke::proxyCall0)
                    .add("values", RMockInvoke::proxyCall0).add("entrySet", RMockInvoke::proxyCall0)
                    .add("getOrDefault", RMockInvoke::proxyCall2).add("forEach", RMockInvoke::proxyCall1)
                    .add("replaceAll", RMockInvoke::proxyCall1).add("putIfAbsent", RMockInvoke::proxyCall2)
                    .add("remove", RMockInvoke::proxyCall1).add("replace", RMockInvoke::proxyCall)
                    .add("computeIfAbsent", RMockInvoke::proxyCall2)
                    .add("computeIfPresent", RMockInvoke::proxyCall2)
                    .add("compute", RMockInvoke::proxyCall2).add("merge", RMockInvoke::proxyCall3);
        }
        return concurrentMap;
    }

    private Number addNumbers(Number a, Number b) {
        if (a instanceof Double || b instanceof Double) {
            return a.doubleValue() + b.doubleValue();
        } else if (a instanceof Float || b instanceof Float) {
            return a.floatValue() + b.floatValue();
        } else if (a instanceof Long || b instanceof Long) {
            return a.longValue() + b.longValue();
        } else {
            return a.intValue() + b.intValue();
        }
    }
}

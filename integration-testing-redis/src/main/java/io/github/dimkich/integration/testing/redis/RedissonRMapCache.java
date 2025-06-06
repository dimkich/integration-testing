package io.github.dimkich.integration.testing.redis;

import org.redisson.api.*;
import org.redisson.api.map.event.MapEntryListener;
import org.redisson.api.mapreduce.RMapReduce;
import org.redisson.client.codec.Codec;
import org.redisson.misc.CompletableFutureWrapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RedissonRMapCache<K, V> extends ConcurrentHashMap<K, V> implements RMapCache<K, V> {
    @Override
    public void setMaxSize(int maxSize) {
    }

    @Override
    public boolean trySetMaxSize(int maxSize) {
        return true;
    }

    @Override
    public V putIfAbsent(K key, V value, long ttl, TimeUnit ttlUnit) {
        return putIfAbsent(key, value);
    }

    @Override
    public V putIfAbsent(K key, V value, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit) {
        return putIfAbsent(key, value);
    }

    @Override
    public V put(K key, V value, long ttl, TimeUnit unit) {
        return put(key, value);
    }

    @Override
    public V put(K key, V value, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit) {
        return put(key, value);
    }

    @Override
    public boolean fastPut(K key, V value, long ttl, TimeUnit ttlUnit) {
        return fastPut(key, value);
    }

    @Override
    public boolean fastPut(K key, V value, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit) {
        return fastPut(key, value);
    }

    @Override
    public boolean fastPutIfAbsent(K key, V value, long ttl, TimeUnit ttlUnit) {
        return fastPutIfAbsent(key, value);
    }

    @Override
    public boolean fastPutIfAbsent(K key, V value, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit) {
        return fastPutIfAbsent(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long ttl, TimeUnit ttlUnit) {
        putAll(map);
    }

    @Override
    public RFuture<Void> putAllAsync(Map<? extends K, ? extends V> map, long ttl, TimeUnit ttlUnit) {
        putAll(map);
        return new CompletableFutureWrapper<>((Void)null);
    }

    @Override
    public int addListener(MapEntryListener listener) {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public void removeListener(int listenerId) {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public long remainTimeToLive(K key) {
        return 0;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void loadAll(boolean replaceExistingValues, int parallelism) {
    }

    @Override
    public void loadAll(Set<? extends K> keys, boolean replaceExistingValues, int parallelism) {
    }

    @Override
    public <KOut, VOut> RMapReduce<K, V, KOut, VOut> mapReduce() {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public RCountDownLatch getCountDownLatch(K key) {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public RPermitExpirableSemaphore getPermitExpirableSemaphore(K key) {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public RSemaphore getSemaphore(K key) {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public RLock getFairLock(K key) {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public RReadWriteLock getReadWriteLock(K key) {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public RLock getLock(K key) {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public int valueSize(K key) {
        return 0;
    }

    @Override
    public V addAndGet(K key, Number delta) {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, int batchSize) {
        putAll(map);
    }

    @Override
    public Map<K, V> getAll(Set<K> keys) {
        return entrySet().stream()
                .filter(e -> keys.contains(e.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    @Override
    public long fastRemove(K... keys) {
        long count = 0;
        for (K key : keys) {
            if (containsKey(key)) {
                count++;
                remove(key);
            }
        }
        return count;
    }

    @Override
    public boolean fastPut(K key, V value) {
        boolean result = !containsKey(key);
        put(key, value);
        return result;
    }

    @Override
    public boolean fastReplace(K key, V value) {
        return fastPut(key, value);
    }

    @Override
    public boolean fastPutIfAbsent(K key, V value) {
        boolean result = !containsKey(key);
        putIfAbsent(key, value);
        return result;
    }

    @Override
    public Set<K> readAllKeySet() {
        return keySet();
    }

    @Override
    public Collection<V> readAllValues() {
        return values();
    }

    @Override
    public Set<Entry<K, V>> readAllEntrySet() {
        return entrySet();
    }

    @Override
    public Map<K, V> readAllMap() {
        return this;
    }

    @Override
    public Set<K> keySet(int count) {
        return keySet();
    }

    @Override
    public Set<K> keySet(String pattern, int count) {
        return keySet();
    }

    @Override
    public Set<K> keySet(String pattern) {
        return keySet();
    }

    @Override
    public Collection<V> values(String keyPattern) {
        return values();
    }

    @Override
    public Collection<V> values(String keyPattern, int count) {
        return values();
    }

    @Override
    public Collection<V> values(int count) {
        return values();
    }

    @Override
    public Set<Entry<K, V>> entrySet(String keyPattern) {
        return entrySet();
    }

    @Override
    public Set<Entry<K, V>> entrySet(String keyPattern, int count) {
        return entrySet();
    }

    @Override
    public Set<Entry<K, V>> entrySet(int count) {
        return entrySet();
    }

    @Override
    public boolean expire(long timeToLive, TimeUnit timeUnit) {
        return true;
    }

    @Override
    public boolean expireAt(long timestamp) {
        return true;
    }

    @Override
    public boolean expireAt(Date timestamp) {
        return true;
    }

    @Override
    public boolean clearExpire() {
        return true;
    }

    @Override
    public long remainTimeToLive() {
        return 0;
    }

    @Override
    public RFuture<Void> setMaxSizeAsync(int maxSize) {
        return new CompletableFutureWrapper<>((Void)null);
    }

    @Override
    public RFuture<Boolean> trySetMaxSizeAsync(int maxSize) {
        return new CompletableFutureWrapper<>((Boolean) null);
    }

    @Override
    public RFuture<V> putIfAbsentAsync(K key, V value, long ttl, TimeUnit unit) {
        return new CompletableFutureWrapper<>(putIfAbsent(key, value));
    }

    @Override
    public RFuture<V> putIfAbsentAsync(K key, V value, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit) {
        return new CompletableFutureWrapper<>(putIfAbsent(key, value));
    }

    @Override
    public RFuture<V> putAsync(K key, V value, long ttl, TimeUnit unit) {
        return new CompletableFutureWrapper<>(put(key, value));
    }

    @Override
    public RFuture<V> putAsync(K key, V value, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit) {
        return new CompletableFutureWrapper<>(put(key, value));
    }

    @Override
    public RFuture<Boolean> fastPutAsync(K key, V value, long ttl, TimeUnit unit) {
        return new CompletableFutureWrapper<>(fastPut(key, value));
    }

    @Override
    public RFuture<Boolean> fastPutAsync(K key, V value, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit) {
        return new CompletableFutureWrapper<>(fastPut(key, value));
    }

    @Override
    public RFuture<Boolean> fastPutIfAbsentAsync(K key, V value, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit) {
        return new CompletableFutureWrapper<>(fastPutIfAbsent(key, value));
    }

    @Override
    public RFuture<Long> remainTimeToLiveAsync(K key) {
        return new CompletableFutureWrapper<>(0L);
    }

    @Override
    public RFuture<Void> loadAllAsync(boolean replaceExistingValues, int parallelism) {
        return new CompletableFutureWrapper<>((Void)null);
    }

    @Override
    public RFuture<Void> loadAllAsync(Set<? extends K> keys, boolean replaceExistingValues, int parallelism) {
        return new CompletableFutureWrapper<>((Void)null);
    }

    @Override
    public RFuture<Integer> valueSizeAsync(K key) {
        return new CompletableFutureWrapper<>(0);
    }

    @Override
    public RFuture<Map<K, V>> getAllAsync(Set<K> keys) {
        return new CompletableFutureWrapper<>(getAll(keys));
    }

    @Override
    public RFuture<Void> putAllAsync(Map<? extends K, ? extends V> map) {
        putAll(map);
        return new CompletableFutureWrapper<>((Void)null);
    }

    @Override
    public RFuture<Void> putAllAsync(Map<? extends K, ? extends V> map, int batchSize) {
        putAll(map);
        return new CompletableFutureWrapper<>((Void)null);
    }

    @Override
    public RFuture<V> addAndGetAsync(K key, Number delta) {
        return new CompletableFutureWrapper<>(addAndGet(key, delta));
    }

    @Override
    public RFuture<Boolean> containsValueAsync(Object value) {
        return new CompletableFutureWrapper<>(containsValue(value));
    }

    @Override
    public RFuture<Boolean> containsKeyAsync(Object key) {
        return new CompletableFutureWrapper<>(containsKey(key));
    }

    @Override
    public RFuture<Integer> sizeAsync() {
        return new CompletableFutureWrapper<>(size());
    }

    @Override
    public RFuture<Long> fastRemoveAsync(K... keys) {
        return new CompletableFutureWrapper<>(fastRemove(keys));
    }

    @Override
    public RFuture<Boolean> fastPutAsync(K key, V value) {
        return new CompletableFutureWrapper<>(fastPut(key, value));
    }

    @Override
    public RFuture<Boolean> fastReplaceAsync(K key, V value) {
        return new CompletableFutureWrapper<>(fastReplace(key, value));
    }

    @Override
    public RFuture<Boolean> fastPutIfAbsentAsync(K key, V value) {
        return new CompletableFutureWrapper<>(fastPutIfAbsent(key, value));
    }

    @Override
    public RFuture<Set<K>> readAllKeySetAsync() {
        return new CompletableFutureWrapper<>(readAllKeySet());
    }

    @Override
    public RFuture<Collection<V>> readAllValuesAsync() {
        return new CompletableFutureWrapper<>(readAllValues());
    }

    @Override
    public RFuture<Set<Entry<K, V>>> readAllEntrySetAsync() {
        return new CompletableFutureWrapper<>(readAllEntrySet());
    }

    @Override
    public RFuture<Map<K, V>> readAllMapAsync() {
        return new CompletableFutureWrapper<>(readAllMap());
    }

    @Override
    public RFuture<V> getAsync(K key) {
        return new CompletableFutureWrapper<>(get(key));
    }

    @Override
    public RFuture<V> putAsync(K key, V value) {
        return new CompletableFutureWrapper<>(put(key, value));
    }

    @Override
    public RFuture<V> removeAsync(K key) {
        return new CompletableFutureWrapper<>(remove(key));
    }

    @Override
    public RFuture<V> replaceAsync(K key, V value) {
        return new CompletableFutureWrapper<>(replace(key, value));
    }

    @Override
    public RFuture<Boolean> replaceAsync(K key, V oldValue, V newValue) {
        return new CompletableFutureWrapper<>(replace(key, oldValue, newValue));
    }

    @Override
    public RFuture<Boolean> removeAsync(Object key, Object value) {
        return new CompletableFutureWrapper<>(remove(key, value));
    }

    @Override
    public RFuture<V> putIfAbsentAsync(K key, V value) {
        return new CompletableFutureWrapper<>(putIfAbsent(key, value));
    }

    @Override
    public RFuture<Boolean> expireAsync(long timeToLive, TimeUnit timeUnit) {
        return new CompletableFutureWrapper<>(true);
    }

    @Override
    public RFuture<Boolean> expireAtAsync(Date timestamp) {
        return new CompletableFutureWrapper<>(true);
    }

    @Override
    public RFuture<Boolean> expireAtAsync(long timestamp) {
        return new CompletableFutureWrapper<>(true);
    }

    @Override
    public RFuture<Boolean> clearExpireAsync() {
        return new CompletableFutureWrapper<>(true);
    }

    @Override
    public RFuture<Long> remainTimeToLiveAsync() {
        return new CompletableFutureWrapper<>(0L);
    }

    @Override
    public Long getIdleTime() {
        return 0L;
    }

    @Override
    public long sizeInMemory() {
        return 0;
    }

    @Override
    public void restore(byte[] state) {
    }

    @Override
    public void restore(byte[] state, long timeToLive, TimeUnit timeUnit) {
    }

    @Override
    public void restoreAndReplace(byte[] state) {
    }

    @Override
    public void restoreAndReplace(byte[] state, long timeToLive, TimeUnit timeUnit) {
    }

    @Override
    public byte[] dump() {
        return new byte[0];
    }

    @Override
    public boolean touch() {
        return false;
    }

    @Override
    public void migrate(String host, int port, int database, long timeout) {
    }

    @Override
    public void copy(String host, int port, int database, long timeout) {
    }

    @Override
    public boolean move(int database) {
        return true;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean delete() {
        return true;
    }

    @Override
    public boolean unlink() {
        return true;
    }

    @Override
    public void rename(String newName) {
    }

    @Override
    public boolean renamenx(String newName) {
        return true;
    }

    @Override
    public boolean isExists() {
        return true;
    }

    @Override
    public Codec getCodec() {
        return null;
    }

    @Override
    public int addListener(ObjectListener listener) {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public RFuture<Long> getIdleTimeAsync() {
        return new CompletableFutureWrapper<>(0L);
    }

    @Override
    public RFuture<Long> sizeInMemoryAsync() {
        return new CompletableFutureWrapper<>(0L);
    }

    @Override
    public RFuture<Void> restoreAsync(byte[] state) {
        return new CompletableFutureWrapper<>((Void)null);
    }

    @Override
    public RFuture<Void> restoreAsync(byte[] state, long timeToLive, TimeUnit timeUnit) {
        return new CompletableFutureWrapper<>((Void)null);
    }

    @Override
    public RFuture<Void> restoreAndReplaceAsync(byte[] state) {
        return new CompletableFutureWrapper<>((Void)null);
    }

    @Override
    public RFuture<Void> restoreAndReplaceAsync(byte[] state, long timeToLive, TimeUnit timeUnit) {
        return new CompletableFutureWrapper<>((Void)null);
    }

    @Override
    public RFuture<byte[]> dumpAsync() {
        return new CompletableFutureWrapper<>((byte[]) null);
    }

    @Override
    public RFuture<Boolean> touchAsync() {
        return new CompletableFutureWrapper<>(true);
    }

    @Override
    public RFuture<Void> migrateAsync(String host, int port, int database, long timeout) {
        return new CompletableFutureWrapper<>((Void)null);
    }

    @Override
    public RFuture<Void> copyAsync(String host, int port, int database, long timeout) {
        return new CompletableFutureWrapper<>((Void)null);
    }

    @Override
    public RFuture<Boolean> moveAsync(int database) {
        return new CompletableFutureWrapper<>(true);
    }

    @Override
    public RFuture<Boolean> deleteAsync() {
        return new CompletableFutureWrapper<>(true);
    }

    @Override
    public RFuture<Boolean> unlinkAsync() {
        return new CompletableFutureWrapper<>(true);
    }

    @Override
    public RFuture<Void> renameAsync(String newName) {
        return null;
    }

    @Override
    public RFuture<Boolean> renamenxAsync(String newName) {
        return new CompletableFutureWrapper<>(true);
    }

    @Override
    public RFuture<Boolean> isExistsAsync() {
        return new CompletableFutureWrapper<>(true);
    }

    @Override
    public RFuture<Integer> addListenerAsync(ObjectListener listener) {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public RFuture<Void> removeListenerAsync(int listenerId) {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public void setMaxSize(int maxSize, EvictionMode mode) {
    }

    @Override
    public boolean trySetMaxSize(int maxSize, EvictionMode mode) {
        return true;
    }

    @Override
    public boolean updateEntryExpiration(K key, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit) {
        return true;
    }

    @Override
    public V getWithTTLOnly(K key) {
        return get(key);
    }




    @Override
    public V putIfExists(K key, V value) {
        return null;
    }

    @Override
    public Set<K> randomKeys(int count) {
        return Set.of();
    }

    @Override
    public Map<K, V> randomEntries(int count) {
        return Map.of();
    }

    @Override
    public boolean fastPutIfExists(K key, V value) {
        return false;
    }

    @Override
    public boolean expire(Instant time) {
        return false;
    }

    @Override
    public boolean expireIfSet(Instant time) {
        return false;
    }

    @Override
    public boolean expireIfNotSet(Instant time) {
        return false;
    }

    @Override
    public boolean expireIfGreater(Instant time) {
        return false;
    }

    @Override
    public boolean expireIfLess(Instant time) {
        return false;
    }

    @Override
    public boolean expire(Duration duration) {
        return false;
    }

    @Override
    public boolean expireIfSet(Duration duration) {
        return false;
    }

    @Override
    public boolean expireIfNotSet(Duration duration) {
        return false;
    }

    @Override
    public boolean expireIfGreater(Duration duration) {
        return false;
    }

    @Override
    public boolean expireIfLess(Duration duration) {
        return false;
    }

    @Override
    public long getExpireTime() {
        return 0;
    }

    @Override
    public RFuture<Void> setMaxSizeAsync(int maxSize, EvictionMode mode) {
        return null;
    }

    @Override
    public RFuture<Boolean> trySetMaxSizeAsync(int maxSize, EvictionMode mode) {
        return null;
    }

    @Override
    public RFuture<Boolean> updateEntryExpirationAsync(K key, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit) {
        return null;
    }

    @Override
    public RFuture<V> getWithTTLOnlyAsync(K key) {
        return null;
    }

    @Override
    public RFuture<Integer> addListenerAsync(MapEntryListener listener) {
        return null;
    }

    @Override
    public RFuture<V> mergeAsync(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return null;
    }

    @Override
    public RFuture<V> computeAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return null;
    }

    @Override
    public RFuture<V> computeIfAbsentAsync(K key, Function<? super K, ? extends V> mappingFunction) {
        return null;
    }

    @Override
    public RFuture<V> computeIfPresentAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return null;
    }

    @Override
    public RFuture<Set<K>> randomKeysAsync(int count) {
        return null;
    }

    @Override
    public RFuture<Map<K, V>> randomEntriesAsync(int count) {
        return null;
    }

    @Override
    public RFuture<Boolean> fastPutIfExistsAsync(K key, V value) {
        return null;
    }

    @Override
    public RFuture<V> putIfExistsAsync(K key, V value) {
        return null;
    }

    @Override
    public RFuture<Boolean> expireAsync(Instant time) {
        return null;
    }

    @Override
    public RFuture<Boolean> expireIfSetAsync(Instant time) {
        return null;
    }

    @Override
    public RFuture<Boolean> expireIfNotSetAsync(Instant time) {
        return null;
    }

    @Override
    public RFuture<Boolean> expireIfGreaterAsync(Instant time) {
        return null;
    }

    @Override
    public RFuture<Boolean> expireIfLessAsync(Instant time) {
        return null;
    }

    @Override
    public RFuture<Boolean> expireAsync(Duration duration) {
        return null;
    }

    @Override
    public RFuture<Boolean> expireIfSetAsync(Duration duration) {
        return null;
    }

    @Override
    public RFuture<Boolean> expireIfNotSetAsync(Duration duration) {
        return null;
    }

    @Override
    public RFuture<Boolean> expireIfGreaterAsync(Duration duration) {
        return null;
    }

    @Override
    public RFuture<Boolean> expireIfLessAsync(Duration duration) {
        return null;
    }

    @Override
    public RFuture<Long> getExpireTimeAsync() {
        return null;
    }
}

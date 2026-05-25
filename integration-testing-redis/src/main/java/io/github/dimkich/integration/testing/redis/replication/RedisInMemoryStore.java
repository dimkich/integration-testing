package io.github.dimkich.integration.testing.redis.replication;

import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.event.EventListener;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import io.github.dimkich.integration.testing.redis.model.RedisEntry;
import io.github.dimkich.integration.testing.redis.model.RedisKey;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaMetadata;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.redis.replication.purge.RedisPurgeBuilder;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import io.github.sugarcubes.cloner.Cloner;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * In-memory mirror of Redis keyspace for a single named connection, updated from replication
 * events via {@link io.github.dimkich.integration.testing.redis.replication.event.listener.RedisEventHandler} delegates.
 * <p>
 * Spring registers one bean per configured Redis instance ({@link io.github.dimkich.integration.testing.redis.config.RedisConfig}).
 * {@link RedisEventDispatcher} and {@link io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSyncStateDelegator}
 * route RDB snapshot and command-stream events into this store; {@link io.github.dimkich.integration.testing.redis.RedisTestDataStorage}
 * reads snapshots and drives virtual time through {@link #updateTimeAndPurge}.
 * <p>
 * Keys are indexed by {@link RedisKey} (logical DB + resolved key string). Schema metadata from
 * {@link RedisDataSchemaRegistry} marks ignored keys and supplies typed {@link RedisDataSchema}
 * instances for {@link #compute} callbacks. Entries are removed automatically when a mutation
 * leaves them empty.
 */
@RequiredArgsConstructor
public class RedisInMemoryStore {
    @Getter
    private final String name;
    @Getter
    private final RedisSyncBarrier barrier;
    private final RedisProperties properties;
    @Getter
    private final Replicator replicator;
    private final Cloner cloner;
    private final RedisDataSchemaRegistry registry;
    private final RedisKeyResolver redisKeyResolver;

    @Getter
    @Setter
    private int currentDb = 0;
    @Getter
    private volatile ZonedDateTime now = ZonedDateTime.now();

    private final Map<RedisKey, RedisEntry> currentValue = new ConcurrentHashMap<>();

    /**
     * Registers a listener on the backing {@link Replicator} (for example sync-phase hooks).
     *
     * @param listener replicator event listener
     */
    public void addEventListener(EventListener listener) {
        replicator.addEventListener(listener);
    }

    /**
     * @return {@code true} if no keys are held in memory across all logical databases
     */
    public boolean isEmpty() {
        return currentValue.isEmpty();
    }

    /**
     * Applies {@code consumer} to the entry for an RDB {@link KeyValuePair}, using the pair's DB
     * number and key and updating TTL from the pair via {@link RedisEntry#setExpireAt}.
     *
     * @param event    snapshot key-value pair from the replicator
     * @param consumer schema-aware mutation; entry is removed if empty afterward
     */
    public void compute(KeyValuePair<byte[], ?> event, BiConsumer<RedisDataSchema, RedisEntry> consumer) {
        compute(event.getDb().getDbNumber(), event.getKey(), consumer.andThen((s, e) -> e.setExpireAt(now, event)));
    }

    /**
     * Like {@link #compute(KeyValuePair, BiConsumer)} but passes {@link RedisEntry#getOrCreateData(Class)}
     * of type {@code cls} to {@code consumer}.
     *
     * @param event    snapshot key-value pair
     * @param cls      expected value type
     * @param consumer schema-aware mutation on typed data
     * @param <T>      value type
     */
    public <T> void compute(KeyValuePair<byte[], ?> event, Class<T> cls, BiConsumer<RedisDataSchema, T> consumer) {
        compute(event, (s, e) -> consumer.accept(s, e.getOrCreateData(cls)));
    }

    /**
     * Applies {@code consumer} on {@link #currentDb} for the raw {@code key}.
     *
     * @param key      raw Redis key bytes
     * @param consumer schema-aware mutation
     */
    public void compute(byte[] key, BiConsumer<RedisDataSchema, RedisEntry> consumer) {
        compute(currentDb, key, consumer);
    }

    /**
     * Typed {@link #compute(byte[], BiConsumer)} on {@link #currentDb}.
     *
     * @param key      raw Redis key bytes
     * @param cls      expected value type
     * @param consumer schema-aware mutation on typed data
     * @param <T>      value type
     */
    public <T> void compute(byte[] key, Class<T> cls, BiConsumer<RedisDataSchema, T> consumer) {
        compute(currentDb, key, cls, consumer);
    }

    /**
     * Typed {@link #compute(long, byte[], BiConsumer)} for an explicit logical database.
     *
     * @param db       logical database index
     * @param key      raw Redis key bytes
     * @param cls      expected value type
     * @param consumer schema-aware mutation on typed data
     * @param <T>      value type
     */
    public <T> void compute(long db, byte[] key, Class<T> cls, BiConsumer<RedisDataSchema, T> consumer) {
        compute(db, key, (s, e) -> consumer.accept(s, e.getOrCreateData(cls)));
    }

    /**
     * Resolves schema metadata, gets or creates a {@link RedisEntry}, runs {@code consumer}, and
     * removes the key if the entry becomes empty. The {@link RedisKey} is marked ignored when the
     * registry says so.
     *
     * @param db       logical database index
     * @param key      raw Redis key bytes
     * @param consumer schema-aware mutation
     */
    public void compute(long db, byte[] key, BiConsumer<RedisDataSchema, RedisEntry> consumer) {
        RedisKey redisKey = getKey(db, key);
        RedisDataSchemaMetadata metadata = registry.findSchema(name, redisKey.getKey().toString());
        RedisEntry entry = currentValue.computeIfAbsent(redisKey, k -> new RedisEntry());
        redisKey.setIgnored(metadata.isIgnore());
        consumer.accept(metadata.getSchema(), entry);
        if (entry.isEmpty()) {
            currentValue.remove(redisKey);
        }
    }

    /**
     * Removes the key from {@link #currentDb} without invoking schema callbacks.
     *
     * @param key raw Redis key bytes
     */
    public void remove(byte[] key) {
        currentValue.remove(getKey(currentDb, key));
    }

    /**
     * @param key raw Redis key bytes on {@link #currentDb}
     * @return {@code true} if an entry exists for the key
     */
    public boolean exists(byte[] key) {
        return currentValue.containsKey(getKey(currentDb, key));
    }

    /**
     * Moves the in-memory entry from {@code oldKeyRaw} to {@code newKeyRaw} on {@link #currentDb}.
     *
     * @param oldKeyRaw previous key bytes
     * @param newKeyRaw new key bytes
     */
    public void rename(byte[] oldKeyRaw, byte[] newKeyRaw) {
        RedisKey oldKeyIdentity = getKey(currentDb, oldKeyRaw);
        RedisKey newKey = getKey(currentDb, newKeyRaw);
        RedisEntry val = currentValue.remove(oldKeyIdentity);
        if (val != null) {
            currentValue.put(newKey, val);
        }
    }

    /**
     * Deep-copies the source entry to the destination on {@link #currentDb}. When {@code replace}
     * is {@code false}, an existing destination entry is left unchanged.
     *
     * @param srcKeyRaw  source key bytes
     * @param destKeyRaw destination key bytes
     * @param replace    if {@code true}, overwrite an existing destination entry
     */
    public void copy(byte[] srcKeyRaw, byte[] destKeyRaw, boolean replace) {
        RedisKey srcKey = getKey(currentDb, srcKeyRaw);
        RedisKey destKey = getKey(currentDb, destKeyRaw);
        RedisEntry val = currentValue.get(srcKey);
        if (val != null) {
            if (replace || !currentValue.containsKey(destKey)) {
                currentValue.put(destKey, cloner.clone(val));
            }
        }
    }

    /** Drops all in-memory keys belonging to {@link #currentDb}. */
    public void flushDb() {
        currentValue.keySet().removeIf(key -> Objects.equals(key.getDb(), currentDb));
    }

    /** Clears the entire in-memory keyspace across all logical databases. */
    public void flushAll() {
        currentValue.clear();
    }

    /**
     * @param key resolved key identity (DB + string key)
     */
    public void removeKey(RedisKey key) {
        currentValue.remove(key);
    }

    /**
     * @param key   resolved key identity
     * @param entry entry to store (not cloned)
     */
    public void putKey(RedisKey key, RedisEntry entry) {
        currentValue.put(key, entry);
    }

    /**
     * @return unmodifiable view of the live key-to-entry map (for tests and diagnostics)
     */
    public Map<RedisKey, RedisEntry> getCurrentValue() {
        return Collections.unmodifiableMap(currentValue);
    }

    /**
     * Advances the virtual clock, detects expired entries in memory, and applies matching {@code DEL}/{@code HDEL}
     * on the backing Redis via {@link RedisPurgeBuilder}.
     *
     * @param newTime            new virtual time used for TTL evaluation
     * @param connectionFactory  connection to the Redis instance under test
     */
    public void updateTimeAndPurge(ZonedDateTime newTime, RedisConnectionFactory connectionFactory) {
        this.now = newTime;
        RedisPurgeBuilder builder = new RedisPurgeBuilder(name, registry, now);
        currentValue.forEach(builder::addCandidate);
        builder.build().execute(connectionFactory);
    }

    /**
     * Waits for replication to catch up via {@link RedisSyncBarrier}, then returns a deep-cloned
     * map of non-ignored keys suitable for assertions. Field exclusions from schema metadata are
     * applied to each value.
     *
     * @return snapshot keyed by {@link RedisKey#toString()}; empty map if the store is empty
     */
    public Map<String, Object> getSnapshot() {
        barrier.triggerAndAwait(properties.getSyncBarrierTimeoutMs());
        if (currentValue.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> snapshot = new HashMap<>();
        currentValue.forEach((redisKey, entry) -> {
            if (redisKey.isIgnored()) {
                return;
            }
            Object clonedValue = cloner.clone(entry);
            RedisDataSchemaMetadata metadata = registry.findSchema(name, redisKey.getKey().toString());
            metadata.getFieldExclusion().process(clonedValue);
            snapshot.put(redisKey.toString(), clonedValue);
        });
        return snapshot;
    }

    /**
     * @param db  logical database index
     * @param key raw Redis key bytes
     * @return resolved {@link RedisKey} for this connection name and database
     */
    public RedisKey getKey(long db, byte[] key) {
        return redisKeyResolver.resolve(name, db, key);
    }
}

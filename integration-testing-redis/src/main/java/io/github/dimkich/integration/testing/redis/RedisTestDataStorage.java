package io.github.dimkich.integration.testing.redis;

import io.github.dimkich.integration.testing.NowSetter;
import io.github.dimkich.integration.testing.initialization.InitializationService;
import io.github.dimkich.integration.testing.initialization.key.value.KeyValueStorageInit;
import io.github.dimkich.integration.testing.redis.accessor.RedisAccessorCoordinator;
import io.github.dimkich.integration.testing.redis.client.RedisClient;
import io.github.dimkich.integration.testing.redis.client.RedisPipeline;
import io.github.dimkich.integration.testing.redis.model.RedisKey;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.serde.RedisDataCodec;
import io.github.dimkich.integration.testing.redis.serde.RedisDataSchema;
import io.github.dimkich.integration.testing.storage.ObjectsDifference;
import io.github.dimkich.integration.testing.storage.SnapshotTestDataStorage;
import io.github.dimkich.integration.testing.storage.keyvalue.KeyValueDataStorage;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link KeyValueDataStorage} backed by a live Redis connection and an in-memory mirror
 * maintained by replication.
 * <p>
 * Spring registers one bean per configured connection
 * ({@link io.github.dimkich.integration.testing.redis.config.RedisConfig}); the bean name is
 * {@code connectionName + "TestDataStorage"}. Test setup writes seed data through
 * {@link #putKeysData(Map)}; assertions read expected state from {@link #doGetCurrentValue()}
 * via the paired {@link io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore}.
 * Virtual time advances through {@link #setNow}, which purges expired keys in the mirror and on Redis.
 * <p>
 * Seed data is written through a single pipeline grouped by logical database: one {@code SELECT}
 * per database and no network round trip per command.
 * <p>
 * {@link #clearAll()} issues {@code FLUSHALL} only when the in-memory store is non-empty,
 * avoiding unnecessary server commands when replication has not yet populated data.
 *
 * @see io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore
 * @see RedisAccessorCoordinator
 * @see KeyValueDataStorage
 */
public class RedisTestDataStorage extends SnapshotTestDataStorage implements KeyValueDataStorage, NowSetter {
    @Getter
    private final String name;
    private final RedisClient client;
    private final RedisInMemoryStore memStore;
    private final RedisDataSchemaRegistry registry;
    private final RedisAccessorCoordinator accessorCoordinator;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private InitializationService initializationService;

    public RedisTestDataStorage(String name, RedisClient client, RedisInMemoryStore memStore,
                                RedisDataSchemaRegistry registry, RedisAccessorCoordinator accessorCoordinator,
                                ObjectsDifference objectsDifference) {
        super(objectsDifference);
        this.name = name;
        this.client = client;
        this.memStore = memStore;
        this.registry = registry;
        this.accessorCoordinator = accessorCoordinator;
    }

    /**
     * Advances virtual time for this Redis instance: purges expired entries in the in-memory store
     * and on the server, then marks this storage as changed so the baseline is refreshed before the
     * test.
     *
     * @param dateTime new "current" time for TTL evaluation and purge
     */
    @Override
    public void setNow(ZonedDateTime dateTime) {
        memStore.updateTimeAndPurge(dateTime, client);
        markAffected();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Runs {@code FLUSHALL} on the connection only when the paired in-memory store is not empty.
     */
    @Override
    public void clearAll() {
        if (!memStore.isEmpty()) {
            client.flushAll();
            markAffected();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Keys may be plain objects or {@link RedisKey} wrappers (logical DB + key). Values are
     * serialized and written through {@link RedisAccessorCoordinator} using the schema from
     * {@link RedisDataSchemaRegistry}. Entries are grouped by database and sent through one
     * pipeline, so the whole fixture is loaded with one round trip per database.
     *
     * @param map key-value pairs to store; no-op when {@code null} or empty
     */
    @Override
    public void putKeysData(Map<Object, Object> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        int defaultDatabase = client.getDefaultDatabase();
        Map<Integer, List<Map.Entry<Object, Object>>> byDatabase = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            int db = defaultDatabase;
            if (entry.getKey() instanceof RedisKey redisKey && redisKey.getDb() != null) {
                db = redisKey.getDb();
            }
            byDatabase.computeIfAbsent(db, key -> new ArrayList<>()).add(entry);
        }

        @Cleanup RedisPipeline pipeline = client.pipeline();
        byDatabase.forEach((db, entries) -> {
            pipeline.select(db);
            storeEntries(entries, pipeline);
        });
        // The pipeline moves the client's selected database on flush, so return to the
        // configured default instead of leaving the connection in the last seeded database.
        pipeline.select(defaultDatabase);
        markAffected();
    }

    private void storeEntries(List<Map.Entry<Object, Object>> entries, RedisPipeline pipeline) {
        RedisDataCodec keyCodec = registry.findKeyCodec(name);
        for (Map.Entry<Object, Object> entry : entries) {
            Object key = entry.getKey();
            if (key instanceof RedisKey redisKey) {
                key = redisKey.getKey();
            }
            byte[] keyRaw = keyCodec.serialize(key);
            RedisDataSchema schema = registry.findSchema(name, entry.getKey().toString()).getSchema();
            accessorCoordinator.store(keyRaw, entry.getValue(), pipeline, schema);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a clone of the in-memory replication snapshot.
     *
     * @return snapshot map from the paired in-memory store
     */
    @Override
    protected Map<String, Object> doGetCurrentValue() {
        return memStore.getSnapshot();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Resets initialization status to {@link KeyValueStorageInit} after the diff.
     */
    @Override
    public Object getDiff() throws Exception {
        Object diff = super.getDiff();
        initializationService.changeCurrentStatus(KeyValueStorageInit.class);
        return diff;
    }
}

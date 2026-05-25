package io.github.dimkich.integration.testing.redis;

import io.github.dimkich.integration.testing.NowSetter;
import io.github.dimkich.integration.testing.initialization.InitializationService;
import io.github.dimkich.integration.testing.initialization.key.value.KeyValueStorageInit;
import io.github.dimkich.integration.testing.redis.accessor.RedisAccessorCoordinator;
import io.github.dimkich.integration.testing.redis.model.RedisKey;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.keyvalue.KeyValueDataStorage;
import lombok.Cleanup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * {@link KeyValueDataStorage} backed by a live Redis connection and an in-memory mirror
 * maintained by replication.
 * <p>
 * Spring registers one bean per configured {@link RedisConnectionFactory}
 * ({@link io.github.dimkich.integration.testing.redis.config.RedisConfig}); the bean name is
 * {@code factoryName + "TestDataStorage"}. Test setup writes seed data through
 * {@link #putKeysData(Map)}; assertions read expected state from {@link #getCurrentValue(Map)}
 * via the paired {@link io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore}.
 * Virtual time advances through {@link #setNow}, which purges expired keys in the mirror and on Redis.
 * <p>
 * {@link #clearAll()} issues {@code FLUSHALL} only when the in-memory store is non-empty,
 * avoiding unnecessary server commands when replication has not yet populated data.
 *
 * @see io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore
 * @see RedisAccessorCoordinator
 * @see KeyValueDataStorage
 */
@RequiredArgsConstructor
public class RedisTestDataStorage implements KeyValueDataStorage, NowSetter {
    @Getter
    private final String name;
    private final RedisConnectionFactory connectionFactory;
    private final RedisInMemoryStore memStore;
    private final RedisDataSchemaRegistry registry;
    private final RedisAccessorCoordinator accessorCoordinator;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private InitializationService initializationService;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private TestDataStorages testDataStorages;

    /**
     * Advances virtual time for this Redis instance: purges expired entries in the in-memory store
     * and on the server, then registers this storage with {@link TestDataStorages} so assertions
     * see the updated snapshot.
     *
     * @param dateTime new "current" time for TTL evaluation and purge
     */
    @Override
    public void setNow(ZonedDateTime dateTime) {
        memStore.updateTimeAndPurge(dateTime, connectionFactory);
        testDataStorages.addAffectedStorage(this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Runs {@code FLUSHALL} on the connection only when the paired in-memory store is not empty.
     */
    @Override
    public void clearAll() {
        if (!memStore.isEmpty()) {
            @Cleanup RedisConnection connection = connectionFactory.getConnection();
            connection.serverCommands().flushAll();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Keys may be plain objects or {@link RedisKey} wrappers (logical DB + key). Values are
     * serialized and written through {@link RedisAccessorCoordinator} using the schema from
     * {@link RedisDataSchemaRegistry}.
     *
     * @param map key-value pairs to store; no-op when {@code null} or empty
     */
    @Override
    public void putKeysData(Map<Object, Object> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        @Cleanup RedisConnection connection = connectionFactory.getConnection();
        int currentDb = 0;

        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Object key = entry.getKey();
            int db = 0;
            if (key instanceof RedisKey redisKey) {
                key = redisKey.getKey();
                db = redisKey.getDb();
            }
            if (!Objects.equals(currentDb, db)) {
                connection.select(db);
                currentDb = db;
            }
            byte[] keyRaw = registry.findKeyCodec(name).getCodec().serialize(key);
            RedisDataSchema schema = registry.findSchema(name, entry.getKey().toString()).getSchema();
            accessorCoordinator.store(keyRaw, entry.getValue(), connection, schema);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a clone of the in-memory replication snapshot; {@code excludedFields} is not applied
     * for Redis storage.
     *
     * @param excludedFields unused for Redis storage
     * @return snapshot map from the paired in-memory store
     */
    @Override
    public Map<String, Object> getCurrentValue(Map<String, Set<String>> excludedFields) {
        return memStore.getSnapshot();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Resets initialization status to {@link KeyValueStorageInit} when the framework detects
     * a diff against expected data.
     *
     * @param diff detected differences; may be {@code null}
     */
    @Override
    public void setDiff(Map<String, Object> diff) {
        initializationService.changeCurrentStatus(KeyValueStorageInit.class);
    }
}

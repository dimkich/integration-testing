package io.github.dimkich.integration.testing.redis.replication.purge;

import io.github.dimkich.integration.testing.redis.model.RedisEntry;
import io.github.dimkich.integration.testing.redis.model.RedisHash;
import io.github.dimkich.integration.testing.redis.model.RedisKey;
import io.github.dimkich.integration.testing.redis.model.RedisValue;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.redis.registry.RedisKeyCodecMetadata;

import java.time.ZonedDateTime;

/**
 * Scans in-memory Redis entries at a virtual clock and collects keys and hash fields whose TTL has expired.
 * <p>
 * Used by {@link io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore#updateTimeAndPurge}
 * to mirror TTL eviction on the backing Redis instance after a test time shift.
 *
 * @see RedisPurgeTask
 * @see RedisDbPurgeTask
 */
public class RedisPurgeBuilder {
    private final ZonedDateTime now;
    private final RedisKeyCodecMetadata keyCodecMeta;
    private final RedisPurgeTask globalTask;

    /**
     * @param storageName connection name used for schema and key-codec lookup
     * @param registry    schema registry for the storage
     * @param now         virtual time at which TTLs are evaluated
     */
    public RedisPurgeBuilder(String storageName, RedisDataSchemaRegistry registry, ZonedDateTime now) {
        this.now = now;
        this.keyCodecMeta = registry.findKeyCodec(storageName);
        this.globalTask = new RedisPurgeTask(storageName, registry, this.keyCodecMeta);
    }

    /**
     * Evaluates TTL on {@code rootEntry} and its nested hash fields; expired entries are queued for deletion.
     *
     * @param redisKey  key metadata (database, ignore flag, logical key)
     * @param rootEntry root value entry whose {@link RedisEntry#setNow} drives expiry detection
     */
    public void addCandidate(RedisKey redisKey, RedisEntry rootEntry) {
        int db = (redisKey.getDb() != null) ? redisKey.getDb() : 0;
        RedisDbPurgeTask dbTask = globalTask.getOrCreateDbTask(db);
        if (rootEntry.setNow(now)) {
            dbTask.addKey(keyCodecMeta.getCodec().serialize(redisKey.getKey()));
        }
        if (rootEntry.getData() instanceof RedisValue structuralValue) {
            int totalSize = (structuralValue instanceof RedisHash hash) ? hash.size() : 0;
            structuralValue.nestedEntries()
                    .filter(entry -> entry.getValue().setNow(now))
                    .forEach(entry -> dbTask.addHashField(redisKey, entry.getKey(), totalSize));
        }
    }

    /** @return aggregated purge task ready for execution against a Redis connection */
    public RedisPurgeTask build() {
        return globalTask;
    }
}
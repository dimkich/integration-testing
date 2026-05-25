package io.github.dimkich.integration.testing.redis.replication;

import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import io.github.dimkich.integration.testing.redis.model.RedisKey;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.redis.registry.RedisKeyCodecMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds {@link RedisKey} instances from raw replication key bytes for a named connection.
 * <p>
 * Deserializes {@code keyRaw} with the connection's key codec from
 * {@link RedisDataSchemaRegistry}, marks the key as ignored when the matching schema has
 * {@link io.github.dimkich.integration.testing.redis.config.RedisProperties.Schema#isIgnore()},
 * and attaches the logical database index only when
 * {@link RedisProperties.Connection#getMultipleDatabases()} is {@code true}.
 * Used by {@link RedisInMemoryStore#getKey(long, byte[])} during snapshot and stream replication.
 *
 * @see RedisKey
 * @see RedisDataSchemaRegistry
 * @see RedisProperties
 */
@Component
@RequiredArgsConstructor
public class RedisKeyResolver {
    private final RedisDataSchemaRegistry registry;
    private final RedisProperties properties;

    /**
     * Resolves a replication key into a {@link RedisKey} for the given connection and database.
     *
     * @param connectionName connection name as defined in {@link RedisProperties}
     * @param db             logical Redis database index from the replicator
     * @param keyRaw         raw key bytes from an RDB entry or replication command
     * @return decoded key with ignore flag set from schema; {@link RedisKey#getDb()} is set when
     *         the connection tracks multiple databases
     * @throws IllegalStateException if the key codec deserializes {@code keyRaw} to {@code null}
     */
    public RedisKey resolve(String connectionName, long db, byte[] keyRaw) {
        RedisKeyCodecMetadata keyMeta = registry.findKeyCodec(connectionName);
        Object decoded = keyMeta.getCodec().deserialize(keyRaw);
        if (decoded == null) {
            throw new IllegalStateException("Redis key deserialized to null.");
        }
        boolean isIgnored = registry.findSchema(connectionName, decoded.toString()).isIgnore();

        RedisKey redisKey = new RedisKey();
        redisKey.setKey(decoded);
        redisKey.setIgnored(isIgnored);

        RedisProperties.Connection conn = properties.getConnection(connectionName);
        if (conn.getMultipleDatabases() != null && conn.getMultipleDatabases()) {
            redisKey.setDb((int) db);
        }
        return redisKey;
    }

}

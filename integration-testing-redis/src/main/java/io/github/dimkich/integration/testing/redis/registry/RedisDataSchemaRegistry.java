package io.github.dimkich.integration.testing.redis.registry;

import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for Redis key codecs and data schemas, resolved per connection.
 * <p>
 * Provides cached access to:
 * <ul>
 *   <li>{@link RedisKeyCodecMetadata} — key serialization and field exclusions for a connection</li>
 *   <li>{@link RedisDataSchemaMetadata} — value/hash serialization rules and schema metadata for a given Redis key</li>
 * </ul>
 * Schemas are selected via longest-prefix matching on key patterns configured in
 * {@link RedisProperties.Connection#getSchemas()}, with a fallback to the connection's default schema.
 *
 * @see RedisKeyCodecMetadata
 * @see RedisDataSchemaMetadata
 * @see ConnectionSchemaRegistry
 * @see RedisProperties
 */
@RequiredArgsConstructor
public class RedisDataSchemaRegistry {
    private final RedisProperties redisProperties;
    private final RedisObjectFactory objectFactory;

    private final Map<String, RedisKeyCodecMetadata> keyCodecCache = new ConcurrentHashMap<>();
    private final Map<String, ConnectionSchemaRegistry> schemaRegistryCache = new ConcurrentHashMap<>();

    /**
     * Returns the key codec metadata for the given connection.
     * Results are cached per connection name.
     *
     * @param connectName the connection name as defined in {@link RedisProperties}
     * @return the key codec metadata, or {@code null} if the connection has no key codec configured
     */
    public RedisKeyCodecMetadata findKeyCodec(String connectName) {
        return keyCodecCache.computeIfAbsent(connectName, cn -> {
            RedisProperties.Connection conn = redisProperties.getConnection(cn);
            return objectFactory.createCodec(conn.getKeyCodec());
        });
    }

    /**
     * Returns the schema metadata for the given connection and Redis key.
     * Schemas are resolved via longest-prefix matching on key patterns configured
     * for the connection; when no pattern matches, the connection's default schema
     * is used. Per-connection registries are cached.
     *
     * @param connectName the connection name as defined in {@link RedisProperties}
     * @param redisKey    the Redis key to look up
     * @return the matching schema metadata (never {@code null})
     */
    public RedisDataSchemaMetadata findSchema(String connectName, String redisKey) {
        ConnectionSchemaRegistry registry = schemaRegistryCache.computeIfAbsent(connectName, cn -> {
            RedisProperties.Connection conn = redisProperties.getConnection(cn);
            TreeMap<String, RedisDataSchemaMetadata> keySchemas = new TreeMap<>();
            if (conn.getSchemas() != null) {
                conn.getSchemas().forEach((pattern, schemaProps) ->
                        keySchemas.put(pattern, objectFactory.createSchema(schemaProps))
                );
            }
            RedisDataSchemaMetadata defaultSchema = objectFactory.createSchema(conn.getDefaultSchema());
            return new ConnectionSchemaRegistry(keySchemas, defaultSchema);
        });
        return registry.findLongestMatchingSchema(redisKey);
    }
}
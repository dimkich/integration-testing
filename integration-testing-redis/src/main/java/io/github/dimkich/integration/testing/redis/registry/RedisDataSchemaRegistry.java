package io.github.dimkich.integration.testing.redis.registry;

import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import io.github.dimkich.integration.testing.redis.serde.RedisDataCodec;
import io.github.dimkich.integration.testing.redis.serde.RedisSchemaSerdeFactory;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for Redis key codecs and data schemas, resolved per connection and cached.
 * Schemas are selected via longest-prefix matching on the key patterns configured in
 * {@link RedisProperties.Connection#getSchemas()}, with a fallback to the connection's default
 * schema.
 *
 * @see RedisDataSchemaMetadata
 * @see ConnectionSchemaRegistry
 * @see RedisProperties
 */
@RequiredArgsConstructor
public class RedisDataSchemaRegistry {
    private final RedisProperties redisProperties;
    private final RedisObjectFactory objectFactory;
    private final RedisSchemaSerdeFactory schemaSerdeFactory;

    private final Map<String, RedisDataCodec> keyCodecCache = new ConcurrentHashMap<>();
    private final Map<String, ConnectionSchemaRegistry> schemaRegistryCache = new ConcurrentHashMap<>();

    /**
     * Returns the key codec for the given connection.
     * Results are cached per connection name.
     *
     * @param connectName the connection name as defined in {@link RedisProperties}
     * @return the key codec, or {@code null} if the connection has no key codec configured
     */
    public RedisDataCodec findKeyCodec(String connectName) {
        return keyCodecCache.computeIfAbsent(connectName, cn -> {
            RedisProperties.Connection conn = redisProperties.getConnection(cn);
            return schemaSerdeFactory.createKeyCodec(conn.getKeyCodec());
        });
    }

    /**
     * Returns the schema metadata for the given connection and Redis key.
     * Schemas are resolved via longest-prefix matching on key patterns configured
     * for the connection; when no pattern matches, the connection's default schema
     * is used. Per-connection registries are cached.
     * <p>
     * Schema inheritance happens at the configuration level: the connection default schema
     * inherits the global default schema, and each key-pattern schema inherits the connection
     * default schema.
     *
     * @param connectName the connection name as defined in {@link RedisProperties}
     * @param redisKey    the Redis key to look up
     * @return the matching schema metadata (never {@code null})
     */
    public RedisDataSchemaMetadata findSchema(String connectName, String redisKey) {
        ConnectionSchemaRegistry registry = schemaRegistryCache.computeIfAbsent(connectName, cn -> {
            RedisProperties.Connection conn = redisProperties.getConnection(cn);
            RedisDataSchemaMetadata defaultSchema = objectFactory.createSchema(conn.getDefaultSchema());
            TreeMap<String, RedisDataSchemaMetadata> keySchemas = new TreeMap<>();
            if (conn.getSchemas() != null) {
                conn.getSchemas().forEach((pattern, schemaProps) ->
                        keySchemas.put(pattern, objectFactory.createSchema(schemaProps)));
            }
            return new ConnectionSchemaRegistry(keySchemas, defaultSchema);
        });
        return registry.findLongestMatchingSchema(redisKey);
    }
}

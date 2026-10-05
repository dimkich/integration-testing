package io.github.dimkich.integration.testing.redis.config;

import io.github.dimkich.integration.testing.config.PropertyInheritanceMerger;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;

/**
 * Configuration properties for Redis integration testing, bound from
 * {@code integration.testing.redis.*}. Global settings are defaults for each named
 * {@link Connection}; connection names match
 * {@link org.springframework.data.redis.connection.RedisConnectionFactory} bean names, and host,
 * port, user and password fall back to {@code embedded.redis.*}.
 *
 * @see RedisConfig
 * @see RedisSchemaProperties
 * @see io.github.dimkich.integration.testing.redis.EnableTestRedis
 */
@Data
@ConfigurationProperties(prefix = "integration.testing.redis", ignoreUnknownFields = false)
public class RedisProperties {
    @Setter(onMethod_ = @Autowired)
    private PropertyInheritanceMerger merger;

    /** Embedded Redis host; used when a {@link Connection} does not define {@code host}. */
    @Value("${embedded.redis.host:}")
    private String host;

    /** Embedded Redis port; used when a {@link Connection} does not define {@code port}. */
    @Value("${embedded.redis.port:}")
    private Integer port;

    /** Embedded Redis username; {@code root} is normalized to {@code null} in {@link #init()}. */
    @Value("${embedded.redis.user:}")
    private String user;

    /** Embedded Redis password; used when a {@link Connection} does not define {@code password}. */
    @Value("${embedded.redis.password:}")
    private String password;

    /**
     * When {@code true}, replication tracks keys per Redis logical database index.
     * Inherited by {@link Connection} instances that do not set their own value.
     */
    private Boolean multipleDatabases;

    /**
     * Maximum time in milliseconds to wait on the per-factory sync barrier during test setup
     * ({@link io.github.dimkich.integration.testing.redis.RedisTestDataStorage}).
     */
    private long syncBarrierTimeoutMs = 5000;

    /** Default key codec for all connections; defaults to the core {@code string} provider. */
    private StandardSerdeProperties keyCodec;

    /**
     * Global default schema for all connections; merged into each {@link Connection#defaultSchema}.
     * Components not set here are defaulted by {@link #init()} to the core {@code string} provider.
     * A connection may override individual components; the unconfigured ones are merged in from
     * this schema.
     */
    private RedisSchemaProperties defaultSchema;

    /** Field paths excluded globally when comparing or processing stored Redis data. */
    private Set<String> excludedFields;

    /**
     * Per-{@link org.springframework.data.redis.connection.RedisConnectionFactory} settings,
     * keyed by factory bean name.
     */
    private Map<String, Connection> connections;

    /**
     * Normalizes credentials, applies the default {@link #keyCodec}, merges each configured
     * {@link #connections} entry with global defaults, and finalizes every configured schema:
     * the record-level base is merged into the components, the default {@code string} source
     * is applied and the result is validated. Defaults are applied after the whole inheritance
     * chain has been merged, so the validated schema is final.
     */
    @PostConstruct
    public void init() {
        this.user = "root".equals(user) ? null : user;
        if (keyCodec == null) {
            keyCodec = new StandardSerdeProperties();
            keyCodec.setType("string");
        }
        if (defaultSchema == null) {
            defaultSchema = new RedisSchemaProperties();
        }
        if (connections == null) {
            return;
        }
        connections.forEach((name, conn) -> {
            prepare(conn);
            applyDefaultSource(conn.getDefaultSchema());
            validate(conn.getDefaultSchema(), "connection[" + name + "].defaultSchema");
            if (conn.getSchemas() != null) {
                conn.getSchemas().forEach((pattern, schema) -> {
                    applyDefaultSource(schema);
                    validate(schema, "connection[" + name + "].schemas[" + pattern + "]");
                });
            }
        });
    }

    private void applyDefaultSource(RedisSchemaProperties schema) {
        if (schema != null) {
            schema.applyDefaultSource(merger);
        }
    }

    private static void validate(RedisSchemaProperties schema, String path) {
        if (schema == null) {
            return;
        }
        try {
            schema.validate();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid schema config at " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns connection settings for {@code name}, creating and merging a new
     * {@link Connection} on first access when absent from {@link #connections}.
     *
     * @param name {@link org.springframework.data.redis.connection.RedisConnectionFactory} bean name
     * @return merged connection properties
     */
    public Connection getConnection(String name) {
        return connections.computeIfAbsent(name, (k) -> prepare(new Connection()));
    }

    private Connection prepare(Connection connection) {
        merger.merge(connection, this);
        if (connection.getSchemas() != null) {
            connection.getSchemas().values().forEach(schema -> {
                merger.merge(schema, connection.getDefaultSchema());
                merger.merge(schema, connection);
            });
        }
        return connection;
    }

    /**
     * Settings for a single Redis connection factory: endpoint credentials, database layout,
     * codecs, schemas, and per-key-pattern schema overrides.
     */
    @Data
    public static class Connection {
        private String host;
        private Integer port;
        private String user;
        private String password;

        /** Overrides global {@link RedisProperties#multipleDatabases} for this factory. */
        private Boolean multipleDatabases;

        /**
         * Key codec for this connection; inherits from global {@link RedisProperties#keyCodec}.
         * A single-slot source: a core serde provider, a Spring Data {@code RedisSerializer}
         * or a Redisson {@code Codec}/{@code RedissonClient}.
         */
        private StandardSerdeProperties keyCodec;

        /**
         * Default {@link RedisSchemaProperties} for keys that do not match any entry in
         * {@link #schemas}; inherits from global {@link RedisProperties#defaultSchema} by the
         * general property-merge rules. Individual components may be overridden; the
         * unconfigured ones are merged in from the global schema.
         */
        private RedisSchemaProperties defaultSchema;

        /** Field exclusions for this connection; merged with global {@link RedisProperties#excludedFields}. */
        private Set<String> excludedFields;

        /**
         * Per-key-pattern {@link RedisSchemaProperties} overrides; map keys are Redis key prefixes
         * resolved by longest-prefix match in
         * {@link io.github.dimkich.integration.testing.redis.registry.ConnectionSchemaRegistry}.
         * Entries inherit from {@link #defaultSchema} and connection-level field exclusions.
         */
        private Map<String, RedisSchemaProperties> schemas;
    }
}

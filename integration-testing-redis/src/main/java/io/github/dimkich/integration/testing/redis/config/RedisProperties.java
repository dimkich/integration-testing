package io.github.dimkich.integration.testing.redis.config;

import io.github.dimkich.integration.testing.config.PropertyInheritanceMerger;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration properties for Redis integration testing, bound from
 * {@code integration.testing.redis.*}. Global settings are defaults for each named
 * {@link Connection}; connection names are logical names configured under
 * {@code integration.testing.redis.connections}.
 * <p>
 * Host, port, user and password have no automatic fallback: they must be configured here or
 * in a connection. Connections themselves must be declared explicitly; there is no implicit
 * default connection.
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

    /** Default Redis host for every connection. */
    private String host;

    /** Default Redis port for every connection. */
    private Integer port;

    /** Redis username; {@code root} and blank values are normalized to {@code null} in {@link #init()}. */
    private String user;

    /** Redis password; used when a {@link Connection} does not define {@code password}. */
    private String password;

    /** Logical database used when a {@code RedisKey} does not carry an explicit index. Defaults to {@code 0}. */
    private Integer database = 0;

    /** Socket connect and read timeout in milliseconds for the framework connection. Defaults to {@code 5000}. */
    private Integer timeoutMs = 5000;

    /** Whether the framework connection uses TLS. Defaults to {@code false}. */
    private Boolean ssl = false;

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
     * Per-connection settings, keyed by the logical connection name configured under
     * {@code integration.testing.redis.connections}.
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
        this.host = blankToNull(host);
        this.user = normalizeUser(user);
        this.password = blankToNull(password);
        if (keyCodec == null) {
            keyCodec = new StandardSerdeProperties();
            keyCodec.setType("string");
        }
        if (defaultSchema == null) {
            defaultSchema = new RedisSchemaProperties();
        }
        if (connections == null) {
            connections = new LinkedHashMap<>();
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * Normalizes a Redis username: blank values and the legacy {@code root} placeholder mean
     * "no username" and become {@code null}.
     */
    private static String normalizeUser(String value) {
        return blankToNull("root".equals(value) ? null : value);
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
     * Returns configuration of a connection declared under
     * {@code integration.testing.redis.connections}.
     *
     * @param name logical connection name configured under {@code integration.testing.redis.connections}
     * @return merged connection properties
     * @throws IllegalStateException when the connection is not configured
     */
    public Connection getConnection(String name) {
        Connection connection = connections == null ? null : connections.get(name);
        if (connection == null) {
            throw new IllegalStateException(
                    String.format("Redis connection '%s' is not configured", name));
        }
        return connection;
    }

    private void prepare(Connection connection) {
        merger.merge(connection, this);
        connection.setHost(blankToNull(connection.getHost()));
        connection.setUser(normalizeUser(connection.getUser()));
        connection.setPassword(blankToNull(connection.getPassword()));
        if (connection.getSchemas() != null) {
            connection.getSchemas().values().forEach(schema -> {
                merger.merge(schema, connection.getDefaultSchema());
                merger.merge(schema, connection);
            });
        }
    }

    /**
     * Settings for a single Redis connection: endpoint credentials, logical database, transport
     * settings, codecs, schemas, and per-key-pattern schema overrides.
     */
    @Data
    public static class Connection {
        private String host;
        private Integer port;
        /** Redis username; {@code root} and blank values are normalized to {@code null}. */
        private String user;
        private String password;

        /** Logical database index used by the framework connection and by keys without an explicit index. */
        private Integer database;

        /** Socket connect and read timeout in milliseconds. */
        private Integer timeoutMs;

        /** Whether the framework connection uses TLS. */
        private Boolean ssl;

        /** Overrides global {@link RedisProperties#multipleDatabases} for this connection. */
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

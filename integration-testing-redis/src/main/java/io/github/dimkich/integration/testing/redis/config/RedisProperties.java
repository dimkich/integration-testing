package io.github.dimkich.integration.testing.redis.config;

import io.github.dimkich.integration.testing.config.PropertyInheritanceExclusive;
import io.github.dimkich.integration.testing.config.PropertyInheritanceMerger;
import io.github.dimkich.integration.testing.redis.codec.StringRedisDataCodec;
import io.github.dimkich.integration.testing.redis.schema.StringRedisDataSchema;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;

/**
 * Configuration properties for Redis integration testing.
 * <p>
 * Bound from {@code integration.testing.redis.*} and enabled by
 * {@link io.github.dimkich.integration.testing.redis.EnableTestRedis} via
 * {@link RedisConfig}. Global settings apply as defaults for each named
 * {@link Connection}; per-connection {@link Schema} entries inherit from the
 * connection's {@link #defaultSchema} and global {@link #defaultSchema} through
 * {@link PropertyInheritanceMerger}.
 * <p>
 * Connection names match {@link org.springframework.data.redis.connection.RedisConnectionFactory}
 * bean names. Host, port, user, and password fall back to {@code embedded.redis.*} when not
 * overridden on a connection.
 *
 * @see RedisConfig
 * @see io.github.dimkich.integration.testing.redis.EnableTestRedis
 */
@Data
@ConfigurationProperties(prefix = "integration.testing.redis")
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

    /** Default key codec for all connections; defaults to {@link StringRedisDataCodec}. */
    private Codec keyCodec;

    /**
     * Global default {@link Schema} for all connections; merged into each {@link Connection#defaultSchema}.
     * When unset, {@link #init()} configures {@link StringRedisDataSchema}.
     */
    private Schema defaultSchema;

    /** Field paths excluded globally when comparing or processing stored Redis data. */
    private Set<String> excludedFields;

    /**
     * Per-{@link org.springframework.data.redis.connection.RedisConnectionFactory} settings,
     * keyed by factory bean name.
     */
    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    private Map<String, Connection> connections;

    /**
     * Normalizes credentials, applies default {@link #keyCodec} and {@link #defaultSchema},
     * and merges each configured {@link #connections} entry with global defaults.
     */
    @PostConstruct
    public void init() {
        this.user = "root".equals(user) ? null : user;
        if (keyCodec == null) {
            keyCodec = new Codec();
            keyCodec.setClassRef(StringRedisDataCodec.class.getName());
        }
        if (defaultSchema == null) {
            defaultSchema = new Schema();
            defaultSchema.setClassRef(StringRedisDataSchema.class.getName());
        }
        if (connections == null) {
            return;
        }
        connections.forEach((name, conn) -> prepare(conn));
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
     * Key codec configuration: Spring bean or class reference, field exclusions, and optional
     * binary envelope format for serialized key bytes.
     */
    @Data
    public static class Codec {
        /** Spring bean name of a {@link io.github.dimkich.integration.testing.redis.codec.RedisDataCodec}. */
        @PropertyInheritanceExclusive("ref")
        private String beanRef;

        /** Fully qualified class name of a {@link io.github.dimkich.integration.testing.redis.codec.RedisDataCodec}. */
        @PropertyInheritanceExclusive("ref")
        private String classRef;

        /** Dot-separated field paths to exclude for keys using this codec. */
        private Set<String> excludedFields;

        /**
         * Binary envelope format for key bytes (parsed by {@link io.github.dimkich.integration.testing.redis.codec.segment.BinaryFormatParser}).
         */
        private String valueBinaryFormat;
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

        /** Key codec for this connection; inherits from global {@link RedisProperties#keyCodec}. */
        private Codec keyCodec;

        /**
         * Default {@link Schema} for keys that do not match any entry in {@link #schemas};
         * inherits from global {@link RedisProperties#defaultSchema}.
         */
        private Schema defaultSchema;

        /** Field exclusions for this connection; merged with global {@link RedisProperties#excludedFields}. */
        private Set<String> excludedFields;

        /**
         * Per-key-pattern {@link Schema} overrides; map keys are Redis key prefixes resolved by
         * longest-prefix match in {@link io.github.dimkich.integration.testing.redis.registry.ConnectionSchemaRegistry}.
         * Entries inherit from {@link #defaultSchema} and connection-level field exclusions.
         */
        private Map<String, Schema> schemas;
    }

    /**
     * Configuration for value and hash serialization on a connection or per-key pattern.
     * <p>
     * At runtime, {@link io.github.dimkich.integration.testing.redis.registry.RedisObjectFactory}
     * resolves {@link #beanRef} or {@link #classRef} to a {@link io.github.dimkich.integration.testing.redis.schema.RedisDataSchema}
     * (via {@link io.github.dimkich.integration.testing.redis.schema.RedisDataSchemaAdapter} when needed),
     * optionally wraps codecs with binary envelopes, and registers the result as
     * {@link io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaMetadata}.
     * Per-pattern entries in {@link Connection#getSchemas()} are matched by
     * {@link io.github.dimkich.integration.testing.redis.registry.ConnectionSchemaRegistry}.
     *
     * @see io.github.dimkich.integration.testing.redis.schema.RedisDataSchema
     * @see io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry
     */
    @Data
    public static class Schema {
        /** Spring bean name of a {@link io.github.dimkich.integration.testing.redis.schema.RedisDataSchema}. */
        @PropertyInheritanceExclusive("ref")
        private String beanRef;

        /** Fully qualified class name of a {@link io.github.dimkich.integration.testing.redis.schema.RedisDataSchema}. */
        @PropertyInheritanceExclusive("ref")
        private String classRef;

        /** When {@code true}, matching keys are skipped in assertions and replication processing. */
        private boolean ignore;

        /** Dot-separated field paths to exclude for values under this schema. */
        private Set<String> excludedFields;

        /** Binary envelope format applied to string/value payloads. */
        private String valueBinaryFormat;

        /** Binary envelope format applied to hash field names. */
        private String hashKeyBinaryFormat;

        /** Binary envelope format applied to hash field values. */
        private String hashValueBinaryFormat;
    }
}

package io.github.dimkich.integration.testing.postgresql.config;

import io.github.dimkich.integration.testing.config.PropertyInheritanceMerger;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration properties for the trigger based PostgreSQL delta storage, bound from
 * {@code integration.testing.postgresql.*}.
 * <p>
 * Each entry of {@link #connections} contributes a storage whose
 * {@link io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService#getName() name}
 * is the connection key. Connection fields that are not set inherit the global settings, which
 * default to the embedded container values ({@code embedded.postgresql.*}).
 */
@Data
@ConfigurationProperties(prefix = "integration.testing.postgresql", ignoreUnknownFields = false)
public class PostgresProperties {
    @Setter(onMethod_ = @Autowired)
    private PropertyInheritanceMerger merger;

    /**
     * Default PostgreSQL host for every connection.
     */
    @Value("${embedded.postgresql.host:}")
    private String host;
    /**
     * Default PostgreSQL port for every connection.
     */
    @Value("${embedded.postgresql.port:}")
    private String port;
    /**
     * Default database name for every connection.
     */
    @Value("${embedded.postgresql.schema:}")
    private String database;
    /**
     * Default PostgreSQL user for every connection.
     */
    @Value("${embedded.postgresql.user:}")
    private String user;
    /**
     * Default PostgreSQL password for every connection.
     */
    @Value("${embedded.postgresql.password:}")
    private String password;

    /**
     * Per-connection settings, keyed by the resulting bean name (for example {@code dataSource}).
     */
    private Map<String, Connection> connections = new LinkedHashMap<>();

    @PostConstruct
    void init() {
        if (connections == null) {
            connections = new LinkedHashMap<>();
        }
        connections.values().forEach(this::prepare);
    }

    private void prepare(Connection connection) {
        merger.merge(connection, this);
        if (!StringUtils.hasText(connection.getDefaultSchema())) {
            connection.setDefaultSchema("public");
        }
    }

    /**
     * Settings for a single PostgreSQL connection.
     */
    @Data
    public static class Connection {
        /**
         * PostgreSQL host; falls back to the global value.
         */
        private String host;
        /**
         * PostgreSQL port; falls back to the global value.
         */
        private String port;
        /**
         * Database name; falls back to the global value. The embedded container exposes it as
         * {@code embedded.postgresql.schema}.
         */
        private String database;
        /**
         * User for PostgreSQL; falls back to the global value.
         */
        private String user;
        /**
         * Password for PostgreSQL; falls back to the global value.
         */
        private String password;
        /**
         * Schema used by {@code TableRegistry} to resolve unqualified table names. The value is
         * required and defaults to {@code public}.
         */
        private String defaultSchema = "public";
        /**
         * Number of rows DBUnit buffers before flushing a JDBC batch when loading fixtures.
         */
        private int batchSize = 10000;
        /**
         * Value used by {@code ALTER SEQUENCE ... RESTART WITH} when sequences are reset around
         * test boundaries. The default {@code 1} keeps SUT generated identifiers deterministic.
         * The value must be greater than explicit fixture identifiers for the table, otherwise the
         * first generated identifier can collide with a fixture row.
         */
        private long sequenceRestartValue = 1;

        /**
         * Per-column configuration. Keys are {@code table.column} or
         * {@code schema.table.column}. In YAML the whole key must be bracket-quoted, for example
         * {@code "[public.users.metadata]"}.
         * <p>
         * A serde override is resolved by the core Serde pipeline and must define a source
         * ({@code type} or {@code bean-ref}). Columns without an override use the built-in
         * converter selected from the PostgreSQL type.
         * <p>
         * The same entry can exclude the column from the storage difference with
         * {@link PostgresColumnProperties#isIgnore()} or exclude nested value fields with
         * {@link PostgresColumnProperties#getExcludedFields()}. Primary-key columns cannot use
         * either setting, because they identify a row.
         */
        private Map<String, PostgresColumnProperties> columns = new LinkedHashMap<>();

        /**
         * Resolves per-column configuration by the canonical {@code schema.table.column} key or
         * the unqualified {@code table.column} key.
         */
        @Nullable
        public PostgresColumnProperties resolveColumn(Table table, String column) {
            if (columns == null || columns.isEmpty()) {
                return null;
            }
            PostgresColumnProperties configured = columns.get(
                    table.getCanonicalName() + "." + column);
            if (configured == null) {
                configured = columns.get(table.getUnqualified() + "." + column);
            }
            return configured;
        }
    }
}

package io.github.dimkich.integration.testing.redis.replication.purge;

import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.redis.registry.RedisKeyCodecMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Executes TTL-driven deletions against a live Redis connection, grouped by logical database.
 * <p>
 * Each database has a {@link RedisDbPurgeTask} that issues pipelined {@code DEL} and {@code HDEL}
 * commands using the connection's key codec and per-key schema codecs.
 *
 * @see RedisPurgeBuilder
 */
@Slf4j
@RequiredArgsConstructor
public class RedisPurgeTask {
    private final String storageName;
    private final RedisDataSchemaRegistry registry;
    private final RedisKeyCodecMetadata keyCodecMeta;
    private final Map<Integer, RedisDbPurgeTask> dbTasks = new HashMap<>();

    /** @param db Redis logical database index */
    public RedisDbPurgeTask getOrCreateDbTask(int db) {
        return dbTasks.computeIfAbsent(db, k -> new RedisDbPurgeTask());
    }

    /**
     * Runs all per-database purge tasks through a single connection, selecting each DB and using a pipeline.
     *
     * @param factory Spring Data Redis connection factory for the target instance
     */
    public void execute(RedisConnectionFactory factory) {
        if (dbTasks.isEmpty()) {
            return;
        }

        try (RedisConnection conn = factory.getConnection()) {
            dbTasks.forEach((db, task) -> {
                try {
                    conn.select(db);
                } catch (UnsupportedOperationException ignore) {
                    log.debug("Redis driver {} select command not supported", factory);
                }
                conn.openPipeline();
                try {
                    task.execute(conn, storageName, registry, keyCodecMeta);
                } finally {
                    conn.closePipeline();
                }
            });
        }
    }
}
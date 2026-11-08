package io.github.dimkich.integration.testing.redis.replication.purge;

import io.github.dimkich.integration.testing.redis.client.RedisClient;
import io.github.dimkich.integration.testing.redis.client.RedisPipeline;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.redis.serde.RedisDataCodec;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Executes TTL-driven deletions against a live Redis connection, grouped by logical database.
 * <p>
 * Each database has a {@link RedisDbPurgeTask} that queues {@code DEL} and {@code HDEL}
 * commands into one pipeline, using the connection's key codec and per-key schema codecs.
 *
 * @see RedisPurgeBuilder
 */
@Slf4j
@RequiredArgsConstructor
public class RedisPurgeTask {
    private final String storageName;
    private final RedisDataSchemaRegistry registry;
    private final RedisDataCodec keyCodec;
    private final Map<Integer, RedisDbPurgeTask> dbTasks = new HashMap<>();

    /** @param db Redis logical database index */
    public RedisDbPurgeTask getOrCreateDbTask(int db) {
        return dbTasks.computeIfAbsent(db, k -> new RedisDbPurgeTask());
    }

    /**
     * Runs all per-database purge tasks through the framework client, one pipeline per database.
     *
     * @param client framework client for the target instance
     */
    public void execute(RedisClient client) {
        if (dbTasks.isEmpty()) {
            return;
        }

        dbTasks.forEach((db, task) -> {
            @Cleanup RedisPipeline pipeline = client.pipeline();
            pipeline.select(db);
            task.execute(pipeline, storageName, registry, keyCodec);
        });
    }
}

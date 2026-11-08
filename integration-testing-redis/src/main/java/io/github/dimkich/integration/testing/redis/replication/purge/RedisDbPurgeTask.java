package io.github.dimkich.integration.testing.redis.replication.purge;

import io.github.dimkich.integration.testing.redis.client.RedisPipeline;
import io.github.dimkich.integration.testing.redis.model.RedisKey;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaMetadata;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.redis.serde.RedisDataCodec;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-database batch of keys and hash fields to delete when their in-memory TTL has expired.
 * <p>
 * When every field of a hash is scheduled for removal, the whole key is deleted with {@code DEL}
 * instead of individual {@code HDEL} calls. Commands are queued into the pipeline as whole lists;
 * the pipeline splits them into chunks and sends one batch.
 * <p>
 * Instances are single-use: {@link #execute} promotes expired hashes into key deletes and moves
 * entries between its collections, so running the same task twice is not supported.
 */
@RequiredArgsConstructor
public class RedisDbPurgeTask {
    private final List<byte[]> keysToDel = new ArrayList<>();
    private final Map<RedisKey, List<Object>> hashFieldsToDel = new LinkedHashMap<>();
    private final Map<RedisKey, Integer> hashTotalSizes = new LinkedHashMap<>();

    /** Queues a top-level key for {@code DEL} (already serialized with the connection key codec). */
    public void addKey(byte[] serializedKey) {
        keysToDel.add(serializedKey);
    }

    /**
     * Queues a hash field for {@code HDEL}, or promotes the entire hash to {@code DEL} when all fields expire.
     *
     * @param rootKey   logical hash key
     * @param field     hash field name (serialized later with the key's schema hash-key codec)
     * @param totalSize current number of fields in the hash (used to detect full-hash expiry)
     */
    public void addHashField(RedisKey rootKey, Object field, int totalSize) {
        hashFieldsToDel.computeIfAbsent(rootKey, k -> new ArrayList<>()).add(field);
        hashTotalSizes.put(rootKey, totalSize);
    }

    /**
     * Promotes full-hash deletes, then queues {@code DEL} and {@code HDEL} commands for the rest.
     * <p>
     * Whole lists are handed to the pipeline, which splits them into command-sized chunks.
     *
     * @param pipeline    open pipeline (caller selects the database)
     * @param storageName connection name for schema lookup
     * @param registry    schema registry for hash-field serialization
     * @param keyCodec    key codec for serializing logical keys to wire format
     */
    public void execute(RedisPipeline pipeline, String storageName, RedisDataSchemaRegistry registry,
                        RedisDataCodec keyCodec) {

        Iterator<Map.Entry<RedisKey, List<Object>>> iterator = hashFieldsToDel.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<RedisKey, List<Object>> entry = iterator.next();
            RedisKey rootKey = entry.getKey();
            List<Object> fields = entry.getValue();
            if (fields.size() >= hashTotalSizes.getOrDefault(rootKey, 0)) {
                keysToDel.add(keyCodec.serialize(rootKey.getKey()));
                iterator.remove();
            }
        }

        if (!keysToDel.isEmpty()) {
            pipeline.del(keysToDel.toArray(new byte[0][]));
        }

        hashFieldsToDel.forEach((rootKey, fields) -> {
            RedisDataSchemaMetadata schemaInfo = registry.findSchema(storageName, rootKey.getKey().toString());
            RedisDataCodec fieldCodec = schemaInfo.getSchema().getHashKeyCodec();
            byte[] rootKeyRaw = keyCodec.serialize(rootKey.getKey());

            byte[][] fieldsRaw = fields.stream()
                    .map(fieldCodec::serialize)
                    .toArray(byte[][]::new);

            pipeline.hDel(rootKeyRaw, fieldsRaw);
        });
    }
}

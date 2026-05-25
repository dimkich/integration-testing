package io.github.dimkich.integration.testing.redis.replication.purge;

import io.github.dimkich.integration.testing.redis.codec.RedisDataCodec;
import io.github.dimkich.integration.testing.redis.model.RedisKey;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaMetadata;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.redis.registry.RedisKeyCodecMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;

import java.util.*;

/**
 * Per-database batch of keys and hash fields to delete when their in-memory TTL has expired.
 * <p>
 * When every field of a hash is scheduled for removal, the whole key is deleted with {@code DEL}
 * instead of individual {@code HDEL} calls. Commands are sent in batches of 500.
 */
@RequiredArgsConstructor
public class RedisDbPurgeTask {
    /** Maximum number of keys or hash fields per pipelined command batch. */
    private static final int BATCH_SIZE = 500;

    private final List<byte[]> keysToDel = new ArrayList<>();
    private final Map<RedisKey, List<Object>> hashFieldsToDel = new HashMap<>();
    private final Map<RedisKey, Integer> hashTotalSizes = new HashMap<>();

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
     * Promotes full-hash deletes, then issues batched {@code DEL} and {@code HDEL} on {@code conn}.
     *
     * @param conn         open Redis connection (caller selects DB and pipeline)
     * @param storageName  connection name for schema lookup
     * @param registry     schema registry for hash-field serialization
     * @param keyCodecMeta key codec for serializing logical keys to wire format
     */
    public void execute(RedisConnection conn, String storageName, RedisDataSchemaRegistry registry,
                        RedisKeyCodecMetadata keyCodecMeta) {

        Iterator<Map.Entry<RedisKey, List<Object>>> iterator = hashFieldsToDel.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<RedisKey, List<Object>> entry = iterator.next();
            RedisKey rootKey = entry.getKey();
            List<Object> fields = entry.getValue();
            if (fields.size() >= hashTotalSizes.getOrDefault(rootKey, 0)) {
                keysToDel.add(keyCodecMeta.getCodec().serialize(rootKey.getKey()));
                iterator.remove();
            }
        }

        for (int i = 0; i < keysToDel.size(); i += BATCH_SIZE) {
            List<byte[]> batch = keysToDel.subList(i, Math.min(i + BATCH_SIZE, keysToDel.size()));
            conn.keyCommands().del(batch.toArray(new byte[0][]));
        }

        hashFieldsToDel.forEach((rootKey, fields) -> {
            RedisDataSchemaMetadata schemaInfo = registry.findSchema(storageName, rootKey.getKey().toString());
            RedisDataCodec fieldCodec = schemaInfo.getSchema().getHashKeyCodec();
            byte[] rootKeyRaw = keyCodecMeta.getCodec().serialize(rootKey.getKey());

            List<byte[]> fieldsRaw = fields.stream()
                    .map(fieldCodec::serialize)
                    .toList();

            for (int j = 0; j < fieldsRaw.size(); j += BATCH_SIZE) {
                List<byte[]> batch = fieldsRaw.subList(j, Math.min(j + BATCH_SIZE, fieldsRaw.size()));
                conn.hashCommands().hDel(rootKeyRaw, batch.toArray(new byte[0][]));
            }
        });
    }
}